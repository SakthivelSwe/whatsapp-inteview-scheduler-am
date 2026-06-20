package com.dummby.interviewscheduler.controller;

import com.dummby.interviewscheduler.util.SampleExcelGenerator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;

/**
 * Convenience endpoint: lets HR download a ready-to-use sample Excel file
 * from the UI without needing the IDE / Maven. Uses the same column layout
 * as {@link SampleExcelGenerator} (RD §3 + extra dynamic columns).
 */
@RestController
@RequestMapping("/api/sample")
@Tag(name = "Sample", description = "Download sample Excel template")
public class SampleDownloadController {

    @Operation(summary = "Download a sample Excel file with all supported columns + 4 demo rows")
    @GetMapping(value = "/excel", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<ByteArrayResource> downloadSample() throws Exception {
        String[] headers = {
                "Phone Number", "Candidate Name", "Job Position",
                "Interview Date", "Interview Time", "Meeting Link",
                "Panel Name", "Job Code", "Recruiter"
        };

        String[][] rows = {
                {"+919342627033", "Godson Robin Raja S", "Angular Developer",
                        "09th June 2026", "11:30 AM", "https://meet.google.com/xwe-ivrc-pet",
                        "Panel A — Frontend", "TVM-NG-001", "Malathi"},
                {"9876543210", "Priya Sharma", "React Developer",
                        "10th June 2026", "10:00 AM", "https://meet.google.com/abc-defg-hij",
                        "Panel A — Frontend", "TVM-RC-014", "Malathi"},
                {"+919900112233", "Rahul Mehta", "Java Developer",
                        "10th June 2026", "02:00 PM", "https://meet.google.com/lmn-5678-stu",
                        "Panel B — Backend", "TVM-JV-007", "Karthik"},
                {"", "Anita R", "HR Executive",
                        "11th June 2026", "11:00 AM", "https://meet.google.com/zzz-9999-aaa",
                        "Panel C — HR", "TVM-HR-002", "Suresh"}  // intentionally invalid (missing phone)
        };

        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("Candidates");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) {
                    Cell cell = row.createCell(c);
                    cell.setCellValue(rows[r][c]);
                }
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);

            ByteArrayResource resource = new ByteArrayResource(out.toByteArray());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"sample-candidates.xlsx\"")
                    .contentType(MediaType.parseMediaType(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .contentLength(out.size())
                    .body(resource);
        }
    }
}

