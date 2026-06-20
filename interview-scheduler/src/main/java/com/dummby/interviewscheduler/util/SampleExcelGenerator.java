package com.dummby.interviewscheduler.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;

/**
 * Generates a sample Excel matching the Requirement Document column spec.
 * Run from your IDE or:
 *   mvn -q exec:java -Dexec.mainClass=com.dummby.interviewscheduler.util.SampleExcelGenerator
 */
public class SampleExcelGenerator {
    public static void main(String[] args) throws Exception {
        String path = args.length > 0 ? args[0] : "sample-candidates.xlsx";

        String[] headers = {
                "Phone Number", "Candidate Name", "Job Position",
                "Interview Date", "Interview Time", "Meeting Link",
                // Extra dynamic columns (auto-detected, no code change required)
                "Panel Name", "Job Code", "Recruiter"
        };

        String[][] rows = {
                {"+919342627033", "Godson Robin Raja S", "Angular Developer",
                        "09th June 2026", "11:30 AM", "https://meet.google.com/xwe-ivrc-pet",
                        "Panel A — Frontend",    "TVM-NG-001",  "Malathi"},
                {"9876543210",    "Priya Sharma",        "React Developer",
                        "10th June 2026", "10:00 AM", "https://meet.google.com/abc-defg-hij",
                        "Panel A — Frontend",    "TVM-RC-014",  "Malathi"},
                {"+919900112233", "Rahul Mehta",         "Java Developer",
                        "10th June 2026", "02:00 PM", "https://meet.google.com/lmn-5678-stu",
                        "Panel B — Backend",     "TVM-JV-007",  "Karthik"},
                // Intentionally invalid: missing phone (to demo validation)
                {"",              "Anita R",             "HR Executive",
                        "11th June 2026", "11:00 AM", "https://meet.google.com/zzz-9999-aaa",
                        "Panel C — HR",          "TVM-HR-002",  "Suresh"}
        };

        try (Workbook wb = new XSSFWorkbook(); FileOutputStream out = new FileOutputStream(path)) {
            Sheet sheet = wb.createSheet("Candidates");
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) header.createCell(i).setCellValue(headers[i]);

            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            wb.write(out);
            System.out.println("Wrote sample Excel to: " + path);
        }
    }
}
