package com.rental.rental_room.billing;
import com.rental.rental_room.common.*;
import com.rental.rental_room.invoice.*;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.math.*;
import java.nio.file.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/invoices") @RequiredArgsConstructor
public class InvoiceExportController {
    private final InvoiceService invoices; private final Database db;
    @Value("${app.pdf.font:}") private String fontPath;
    @GetMapping(value="/{id}/pdf",produces=MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> pdf(@PathVariable Long id) throws IOException {
        var invoice=invoices.get(id);
        try(var doc=new PDDocument();var out=new ByteArrayOutputStream()) {
            var page=new PDPage();doc.addPage(page);
            var font=PDType0Font.load(doc,findFont());
            try(var stream=new PDPageContentStream(doc,page)) {
                stream.beginText();stream.setFont(font,12);stream.newLineAtOffset(48,750);
                var lines=new ArrayList<String>();
                lines.add("RENTAL INVOICE "+invoice.number());
                lines.add("Room ID: "+invoice.roomId()+"   Period: "+invoice.month()+"/"+invoice.year());
                lines.add("Due: "+invoice.dueDate()+"   Status: "+invoice.status());
                lines.add("");
                for(var item:invoice.items())lines.add(item.description()+" | "+item.quantity()+" x "+item.unitPrice()+" = "+item.amount());
                lines.add("");lines.add("TOTAL: "+invoice.totalAmount()+" VND");
                lines.add("PAID: "+invoice.paidAmount()+" VND");lines.add("OUTSTANDING: "+invoice.outstandingAmount()+" VND");
                if(lines.size()>40)throw BusinessException.invalid("Too many invoice items for one-page PDF");
                for(var line:lines) {stream.showText(line);stream.newLineAtOffset(0,-18);}
                stream.endText();
            }
            doc.save(out);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\""+invoice.number()+".pdf\"")
                .body(out.toByteArray());
        }
    }
    private File findFont() {
        for(String candidate:List.of(fontPath,"C:/Windows/Fonts/arial.ttf",
            "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf")) {
            if(!candidate.isBlank()&&Files.isRegularFile(Path.of(candidate)))return new File(candidate);
        }
        throw BusinessException.invalid("No Unicode PDF font available; configure APP_PDF_FONT");
    }
    @GetMapping(value="/export.xlsx",produces="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @Transactional(readOnly=true)
    public ResponseEntity<byte[]> excel(@RequestParam @Min(1) @Max(12) int month,@RequestParam @Min(2000) @Max(9999) int year,
        @RequestParam(required=false) Long propertyId) throws IOException {
        var found=db.query("select i from Invoice i join fetch i.room where i.month=:month and i.year=:year and (:property is null or i.room.property.id=:property) order by i.room.code",
            Invoice.class,"month",month,"year",year,"property",propertyId);
        var paid=new HashMap<Long,BigDecimal>();var ids=found.stream().map(Invoice::getId).toList();
        if(!ids.isEmpty())for(var row:db.query("select p.invoice.id,sum(p.amount) from Payment p where p.invoice.id in :ids group by p.invoice.id",
            Object[].class,"ids",ids))paid.put((Long)row[0],(BigDecimal)row[1]);
        try(var workbook=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=workbook.createSheet("Invoices");int n=0;
            var header=sheet.createRow(n++);
            String[] headings={"Invoice","Room","Month","Year","Rent","Electric","Water","Service","Other","Discount","Total","Paid","Outstanding","Status"};
            for(int j=0;j<headings.length;j++)header.createCell(j).setCellValue(headings[j]);
            for(var i:found) {
                var row=sheet.createRow(n++);int col=0;
                row.createCell(col++).setCellValue(i.getNumber());row.createCell(col++).setCellValue(i.getRoom().getCode());
                row.createCell(col++).setCellValue(i.getMonth());row.createCell(col++).setCellValue(i.getYear());
                for(BigDecimal v:List.of(i.getRoomAmount(),i.getElectricAmount(),i.getWaterAmount(),i.getServiceAmount(),
                    i.getOtherAmount(),i.getDiscount(),i.getTotalAmount(),paid.getOrDefault(i.getId(),Money.ZERO),
                    i.getTotalAmount().subtract(paid.getOrDefault(i.getId(),Money.ZERO))))
                    row.createCell(col++).setCellValue(v.doubleValue());
                row.createCell(col).setCellValue(i.getStatus().name());
            }
            sheet.setColumnWidth(0,7000);sheet.setColumnWidth(1,3000);
            workbook.write(out);
            return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"invoices-"+year+"-"+String.format("%02d",month)+".xlsx\"").body(out.toByteArray());
        }
    }
}
