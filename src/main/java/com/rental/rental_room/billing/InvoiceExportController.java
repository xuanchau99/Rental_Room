package com.rental.rental_room.billing;

import com.rental.rental_room.common.*;
import com.rental.rental_room.config.AppProperties;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class InvoiceExportController {
  private final InvoiceService invoices;
  private final InvoiceExportDataService exportData;
  private final AppProperties properties;

  @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
  public ResponseEntity<byte[]> pdf(@PathVariable Long id) throws IOException {
    var invoice = invoices.get(id);
    try (var doc = new PDDocument();
        var out = new ByteArrayOutputStream();
        var fontStream = openFont()) {
      var page = new PDPage();
      doc.addPage(page);
      var font = PDType0Font.load(doc, fontStream);
      try (var stream = new PDPageContentStream(doc, page)) {
        stream.beginText();
        stream.setFont(font, 12);
        stream.newLineAtOffset(48, 750);
        var lines = new ArrayList<String>();
        lines.add("RENTAL INVOICE " + invoice.number());
        lines.add(
            "Room ID: "
                + invoice.roomId()
                + "   Period: "
                + invoice.month()
                + "/"
                + invoice.year());
        lines.add("Due: " + invoice.dueDate() + "   Status: " + invoice.status());
        lines.add("");
        for (var item : invoice.items())
          lines.add(
              item.description()
                  + " | "
                  + item.quantity()
                  + " x "
                  + item.unitPrice()
                  + " = "
                  + item.amount());
        lines.add("");
        lines.add("TOTAL: " + invoice.totalAmount() + " VND");
        lines.add("PAID: " + invoice.paidAmount() + " VND");
        lines.add("OUTSTANDING: " + invoice.outstandingAmount() + " VND");
        if (lines.size() > 40)
          throw BusinessException.invalid("Too many invoice items for one-page PDF");
        for (var line : lines) {
          stream.showText(line);
          stream.newLineAtOffset(0, -18);
        }
        stream.endText();
      }
      doc.save(out);
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"" + invoice.number() + ".pdf\"")
          .body(out.toByteArray());
    }
  }

  private InputStream openFont() throws IOException {
    var fontPath = properties.pdf().font();
    if (!fontPath.isBlank()) {
      var path = Path.of(fontPath);
      if (!Files.isRegularFile(path))
        throw BusinessException.invalid("APP_PDF_FONT does not point to a font file");
      return Files.newInputStream(path);
    }
    var bundled = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf");
    if (bundled == null) throw new IllegalStateException("Bundled PDF font is missing");
    return bundled;
  }

  @GetMapping(
      value = "/export.xlsx",
      produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
  public ResponseEntity<byte[]> excel(
      @RequestParam int month,
      @RequestParam int year,
      @RequestParam(required = false) Long propertyId)
      throws IOException {
    if (month < 1 || month > 12) throw BusinessException.invalid("month must be between 1 and 12");
    if (year < 2000 || year > 9999)
      throw BusinessException.invalid("year must be between 2000 and 9999");
    var found = exportData.rows(month, year, propertyId);
    try (var workbook = new XSSFWorkbook();
        var out = new ByteArrayOutputStream()) {
      var sheet = workbook.createSheet("Invoices");
      int n = 0;
      var header = sheet.createRow(n++);
      String[] headings = {
        "Invoice",
        "Room",
        "Month",
        "Year",
        "Rent",
        "Electric",
        "Water",
        "Service",
        "Other",
        "Discount",
        "Total",
        "Paid",
        "Outstanding",
        "Status"
      };
      for (int j = 0; j < headings.length; j++) header.createCell(j).setCellValue(headings[j]);
      for (var i : found) {
        var row = sheet.createRow(n++);
        int col = 0;
        row.createCell(col++).setCellValue(i.number());
        row.createCell(col++).setCellValue(i.roomCode());
        row.createCell(col++).setCellValue(i.month());
        row.createCell(col++).setCellValue(i.year());
        for (BigDecimal v :
            List.of(
                i.roomAmount(),
                i.electricAmount(),
                i.waterAmount(),
                i.serviceAmount(),
                i.otherAmount(),
                i.discount(),
                i.totalAmount(),
                i.paidAmount(),
                i.outstandingAmount())) row.createCell(col++).setCellValue(v.doubleValue());
        row.createCell(col).setCellValue(i.status());
      }
      sheet.setColumnWidth(0, 7000);
      sheet.setColumnWidth(1, 3000);
      workbook.write(out);
      return ResponseEntity.ok()
          .header(
              HttpHeaders.CONTENT_DISPOSITION,
              "attachment; filename=\"invoices-"
                  + year
                  + "-"
                  + String.format("%02d", month)
                  + ".xlsx\"")
          .body(out.toByteArray());
    }
  }
}
