package com.rental.rental_room.electricity;

import com.rental.rental_room.common.*;
import com.rental.rental_room.config.AppProperties;
import com.rental.rental_room.room.Room;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.YearMonth;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/electricity-readings")
@RequiredArgsConstructor
public class ElectricityEvidenceController {
  private final EvidenceService evidence;

  public record Correction(
      @NotNull @DecimalMin("0") BigDecimal currentReading,
      @NotBlank @Size(max = 1000) String reason) {}

  public record History(
      Long id,
      BigDecimal oldReading,
      BigDecimal newReading,
      String changedBy,
      String reason,
      java.time.Instant changedAt) {}

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('OWNER')")
  public com.rental.rental_room.api.Views.ReadingView revise(
      @PathVariable Long id, @Valid @RequestBody Correction r, @AuthenticationPrincipal Jwt jwt) {
    return evidence.revise(id, r, jwt.getSubject());
  }

  @GetMapping("/{id}/history")
  public List<History> history(@PathVariable Long id) {
    return evidence.history(id);
  }

  @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public Map<String, String> upload(@PathVariable Long id, @RequestParam("file") MultipartFile file)
      throws IOException {
    return evidence.upload(id, file);
  }

  @GetMapping(value = "/{id}/image")
  public ResponseEntity<byte[]> download(@PathVariable Long id) throws IOException {
    return evidence.download(id);
  }
}

@Service
@RequiredArgsConstructor
class EvidenceService {
  private final Database db;
  private final PlatformTransactionManager transactionManager;
  private final AppProperties properties;

  @Transactional
  public com.rental.rental_room.api.Views.ReadingView revise(
      Long id, ElectricityEvidenceController.Correction input, String actor) {
    var first = db.get(ElectricityMeter.class, id);
    db.lock(Room.class, first.getRoom().getId());
    var reading = db.lock(ElectricityMeter.class, id);
    var period = YearMonth.of(reading.getYear(), reading.getMonth());
    if (db.first(
            "select i from com.rental.rental_room.invoice.Invoice i where i.room.id=:room and i.year=:year and i.month=:month",
            com.rental.rental_room.invoice.Invoice.class,
            "room",
            reading.getRoom().getId(),
            "year",
            reading.getYear(),
            "month",
            reading.getMonth())
        .isPresent())
      throw BusinessException.conflict("Invoice already issued; reading cannot be changed");
    if (db.first(
            "select e from ElectricityMeter e where e.room.id=:room and e.year=:year and e.month=:month",
            ElectricityMeter.class,
            "room",
            reading.getRoom().getId(),
            "year",
            period.plusMonths(1).getYear(),
            "month",
            period.plusMonths(1).getMonthValue())
        .isPresent())
      throw BusinessException.conflict(
          "Next month already exists; correct it first or use a manual adjustment");
    if (input.currentReading().compareTo(reading.getPreviousReading()) < 0)
      throw BusinessException.invalid("New reading is below previous reading");
    var revision = new ElectricityRevision();
    revision.setReading(reading);
    revision.setOldReading(reading.getCurrentReading());
    revision.setNewReading(input.currentReading());
    revision.setChangedBy(actor);
    revision.setReason(input.reason());
    db.save(revision);
    reading.setCurrentReading(input.currentReading());
    reading.setReading(input.currentReading());
    reading.setUsage(input.currentReading().subtract(reading.getPreviousReading()));
    reading.setAmount(Money.charge(reading.getUsage(), reading.getUnitPrice()));
    return com.rental.rental_room.api.Views.ReadingView.of(reading);
  }

  @Transactional(readOnly = true)
  public List<ElectricityEvidenceController.History> history(Long id) {
    db.get(ElectricityMeter.class, id);
    return db
        .query(
            "select h from ElectricityRevision h where h.reading.id=:id order by h.id",
            ElectricityRevision.class,
            "id",
            id)
        .stream()
        .map(
            h ->
                new ElectricityEvidenceController.History(
                    h.getId(),
                    h.getOldReading(),
                    h.getNewReading(),
                    h.getChangedBy(),
                    h.getReason(),
                    h.getCreatedAt()))
        .toList();
  }

  public Map<String, String> upload(Long id, MultipartFile file) throws IOException {
    var bytes = file.getBytes();
    if (bytes.length < 8 || bytes.length > 2_000_000)
      throw BusinessException.invalid("Image must be 1..2 MB");
    String ext;
    if (bytes[0] == (byte) 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4e && bytes[3] == 0x47)
      ext = ".png";
    else if (bytes[0] == (byte) 0xff && bytes[1] == (byte) 0xd8) ext = ".jpg";
    else throw BusinessException.invalid("Only JPEG and PNG images are accepted");
    var directory = Path.of(properties.images().root()).toAbsolutePath().normalize();
    Files.createDirectories(directory);
    var name = UUID.randomUUID() + ext;
    var image = directory.resolve(name);
    try {
      Files.write(image, bytes, StandardOpenOption.CREATE_NEW);
      var oldName =
          new TransactionTemplate(transactionManager)
              .execute(
                  status -> {
                    var reading = db.lock(ElectricityMeter.class, id);
                    var previous = reading.getImageUrl();
                    reading.setImageUrl(name);
                    return previous;
                  });
      if (oldName != null && oldName.matches("[a-f0-9-]{36}\\.(png|jpg)")) {
        try {
          Files.deleteIfExists(directory.resolve(oldName));
        } catch (IOException ignored) {
          /* An old image can be cleaned up later without failing the saved upload. */
        }
      }
    } catch (RuntimeException | Error | IOException ex) {
      try {
        Files.deleteIfExists(image);
      } catch (IOException cleanup) {
        ex.addSuppressed(cleanup);
      }
      throw ex;
    }
    return Map.of("imageUrl", "/api/v1/electricity-readings/" + id + "/image");
  }

  public ResponseEntity<byte[]> download(Long id) throws IOException {
    var transaction = new TransactionTemplate(transactionManager);
    transaction.setReadOnly(true);
    var name = transaction.execute(status -> db.get(ElectricityMeter.class, id).getImageUrl());
    if (name == null || !name.matches("[a-f0-9-]{36}\\.(png|jpg)"))
      throw BusinessException.missing("No uploaded image");
    var file = Path.of(properties.images().root()).toAbsolutePath().normalize().resolve(name);
    if (!Files.isRegularFile(file)) throw BusinessException.missing("Image file missing");
    return ResponseEntity.ok()
        .contentType(name.endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG)
        .body(Files.readAllBytes(file));
  }
}
