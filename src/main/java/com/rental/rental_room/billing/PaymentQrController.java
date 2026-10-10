package com.rental.rental_room.billing;

import com.google.zxing.*;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.rental.rental_room.common.*;
import com.rental.rental_room.config.AppProperties;
import java.io.*;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/invoices")
@RequiredArgsConstructor
public class PaymentQrController {
  private final InvoiceService invoices;
  private final AppProperties properties;

  @GetMapping(value = "/{id}/payment-qr.png", produces = MediaType.IMAGE_PNG_VALUE)
  public byte[] qr(@PathVariable Long id) throws IOException, WriterException {
    var bin = properties.bank().bin();
    var account = properties.bank().account();
    var name = properties.bank().name();
    if (!bin.matches("[0-9]{6}") || !account.matches("[0-9]{6,19}") || name.isBlank())
      throw BusinessException.conflict(
          "Configure BANK_BIN, BANK_ACCOUNT and BANK_NAME to enable transfer QR");
    var invoice = invoices.get(id);
    if (invoice.outstandingAmount().signum() <= 0)
      throw BusinessException.conflict("Invoice has no outstanding balance");
    if (invoice.outstandingAmount().stripTrailingZeros().scale() > 0)
      throw BusinessException.conflict("VietQR transfer amount must be whole VND");
    String beneficiary = tlv("00", bin) + tlv("01", account);
    String provider = tlv("00", "A000000727") + tlv("01", beneficiary) + tlv("02", "QRIBFTTA");
    String merchant = ascii(name).toUpperCase(Locale.ROOT);
    merchant = merchant.substring(0, Math.min(25, merchant.length()));
    String text =
        tlv("00", "01")
            + tlv("01", "12")
            + tlv("38", provider)
            + tlv("52", "0000")
            + tlv("53", "704")
            + tlv("54", invoice.outstandingAmount().toBigIntegerExact().toString())
            + tlv("58", "VN")
            + tlv("59", merchant)
            + tlv("60", "HANOI")
            + tlv(
                "62",
                tlv("05", invoice.number().substring(0, Math.min(25, invoice.number().length()))));
    String prefix = text + "6304";
    int crc = 0xFFFF;
    for (byte b : prefix.getBytes(StandardCharsets.US_ASCII)) {
      crc ^= (b & 0xFF) << 8;
      for (int i = 0; i < 8; i++)
        crc = (crc & 0x8000) != 0 ? ((crc << 1) ^ 0x1021) & 0xFFFF : (crc << 1) & 0xFFFF;
    }
    String payload = prefix + String.format("%04X", crc);
    var matrix = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, 400, 400);
    try (var out = new ByteArrayOutputStream()) {
      MatrixToImageWriter.writeToStream(matrix, "PNG", out);
      return out.toByteArray();
    }
  }

  private static String tlv(String tag, String value) {
    if (value.length() > 99) throw BusinessException.invalid("QR field too long");
    return tag + String.format("%02d", value.length()) + value;
  }

  private static String ascii(String input) {
    return Normalizer.normalize(input, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .replace('Đ', 'D')
        .replace('đ', 'd')
        .replaceAll("[^A-Za-z0-9 ]", " ")
        .trim();
  }
}
