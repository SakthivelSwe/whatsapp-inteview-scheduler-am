package com.dummby.interviewscheduler.service;

import com.dummby.interviewscheduler.model.entity.Candidate;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Parses an uploaded Excel file (.xlsx) into Candidate entities.
 *
 * <h3>Dynamic header support</h3>
 * The Excel file may contain <b>any</b> set of columns — the parser does NOT
 * require a fixed schema. The only system-mandatory column is <b>Phone Number</b>
 * (any header that resolves to {@code phoneNumber}).
 *
 * <p>Every column in the Excel becomes <b>two</b> placeholders the user can
 * reference in templates:
 * <ul>
 *   <li><code>{{column_N}}</code> — positional, where N is the 1-based column index
 *       in the Excel (excluding the Phone Number column).</li>
 *   <li><code>{{slug}}</code> — slugified version of the header
 *       (e.g. "Panel Name" → <code>panel_name</code>, "Job Code" → <code>job_code</code>).</li>
 * </ul>
 *
 * <p>Well-known headers (Candidate Name, Job Position, Interview Date,
 * Interview Time, Meeting Link) are also persisted to dedicated columns for
 * backwards compatibility, BUT new/unknown headers (Panel Name, Job Code,
 * Round, Recruiter, Salary, …) are kept transparently in {@code extraFields}.
 *
 * <p>Validation: only Phone Number must be present and well-formed in the
 * dynamic-header model.
 */
@Slf4j
@Service
public class ExcelParserService {

    /** Default header → internal field name. Keys are normalized (lowercase, no spaces). */
    private static final Map<String, String> KNOWN_HEADER_MAP = Map.ofEntries(
            Map.entry("phonenumber",    "phoneNumber"),
            Map.entry("phone",          "phoneNumber"),
            Map.entry("mobile",         "phoneNumber"),
            Map.entry("whatsapp",       "phoneNumber"),
            Map.entry("whatsappnumber", "phoneNumber"),
            Map.entry("candidatename",  "candidateName"),
            Map.entry("name",           "candidateName"),
            Map.entry("fullname",       "candidateName"),
            Map.entry("jobposition",    "jobPosition"),
            Map.entry("position",       "jobPosition"),
            Map.entry("role",           "jobPosition"),
            Map.entry("interviewdate",  "interviewDate"),
            Map.entry("date",           "interviewDate"),
            Map.entry("interviewtime",  "interviewTime"),
            Map.entry("time",           "interviewTime"),
            Map.entry("meetinglink",    "meetingLink"),
            Map.entry("gmeetlink",      "meetingLink"),
            Map.entry("googlemeetlink", "meetingLink"),
            Map.entry("link",           "meetingLink")
    );

    @Getter
    public static class ParseResult {
        private final List<Candidate> all = new ArrayList<>();
        private final List<Candidate> valid = new ArrayList<>();
        private final List<Candidate> invalid = new ArrayList<>();
        /** Detected schema: one entry per Excel column with header + slug + positional placeholder. */
        private final List<DetectedColumn> schema = new ArrayList<>();
    }

    @Getter
    public static class DetectedColumn {
        private final int columnIndex;          // 0-based Excel column
        private final int positionalIndex;      // 1-based, EXCLUDES the phone column (-1 for phone)
        private final String header;            // original header text
        private final String slug;              // slugified placeholder key
        private final String positionalPlaceholder; // {{column_N}} (null for phone)
        private final String namedPlaceholder;  // {{slug}}
        private final String mappedField;       // legacy field name if known, else null
        private final boolean isPhone;

        public DetectedColumn(int columnIndex, int positionalIndex, String header,
                              String slug, String mappedField, boolean isPhone) {
            this.columnIndex = columnIndex;
            this.positionalIndex = positionalIndex;
            this.header = header;
            this.slug = slug;
            this.positionalPlaceholder = isPhone ? null : "{{column_" + positionalIndex + "}}";
            this.namedPlaceholder = "{{" + slug + "}}";
            this.mappedField = mappedField;
            this.isPhone = isPhone;
        }
    }

    public ParseResult parse(MultipartFile file, Map<String, String> customMapping) throws IOException {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) throw new IllegalArgumentException("Excel file has no sheet");

            Iterator<Row> rows = sheet.iterator();
            if (!rows.hasNext()) throw new IllegalArgumentException("Excel file is empty");

            ParseResult result = new ParseResult();
            DataFormatter fmt = new DataFormatter();

            List<DetectedColumn> columns = buildSchema(rows.next(), customMapping, fmt);
            result.schema.addAll(columns);

            if (columns.stream().noneMatch(DetectedColumn::isPhone)) {
                throw new IllegalArgumentException(
                        "Excel must contain a Phone Number column. Detected headers: "
                                + columns.stream().map(DetectedColumn::getHeader).toList());
            }

            int rowNum = 1;
            while (rows.hasNext()) {
                Row row = rows.next();
                rowNum++;
                if (isRowEmpty(row, fmt)) continue;

                Map<String, String> raw = readRow(row, columns, fmt);

                Candidate c = Candidate.builder()
                        .rowNumber(rowNum)
                        .candidateName(raw.get("candidateName"))
                        .phoneNumber(normalizePhone(raw.get("phoneNumber")))
                        .jobPosition(raw.get("jobPosition"))
                        .interviewDate(raw.get("interviewDate"))
                        .interviewTime(raw.get("interviewTime"))
                        .meetingLink(raw.get("meetingLink"))
                        .build();

                // Build dynamic field map: positional + slug keys for ALL non-phone columns
                Map<String, String> extras = new LinkedHashMap<>();
                for (DetectedColumn col : columns) {
                    if (col.isPhone()) continue;
                    Cell cell = row.getCell(col.getColumnIndex());
                    String val = cell == null ? "" : fmt.formatCellValue(cell).trim();
                    extras.put("column_" + col.getPositionalIndex(), val);
                    extras.put(col.getSlug(), val);
                }
                c.setExtraFields(extras);

                String error = validate(c);
                if (error != null) {
                    c.setStatus(Candidate.SendStatus.INVALID);
                    c.setValidationError(error);
                    result.invalid.add(c);
                } else {
                    c.setStatus(Candidate.SendStatus.PENDING);
                    result.valid.add(c);
                }
                result.all.add(c);
            }

            log.info("Parsed {}: total={}, valid={}, invalid={}, columns={}",
                    file.getOriginalFilename(), result.all.size(), result.valid.size(),
                    result.invalid.size(), columns.size());
            return result;
        }
    }

    /* ---------- helpers ---------- */

    private List<DetectedColumn> buildSchema(Row headerRow, Map<String, String> customMapping, DataFormatter fmt) {
        List<DetectedColumn> columns = new ArrayList<>();
        Set<String> usedSlugs = new HashSet<>();
        int positional = 0;

        for (int i = 0; i < headerRow.getLastCellNum(); i++) {
            Cell cell = headerRow.getCell(i);
            if (cell == null) continue;
            String header = fmt.formatCellValue(cell).trim();
            if (header.isEmpty()) continue;

            String normalized = header.toLowerCase().replaceAll("\\s+", "");

            String mappedField = null;
            if (customMapping != null) {
                if (customMapping.containsKey(header)) mappedField = customMapping.get(header);
                else if (customMapping.containsKey(normalized)) mappedField = customMapping.get(normalized);
            }
            if (mappedField == null) mappedField = KNOWN_HEADER_MAP.get(normalized);

            boolean isPhone = "phoneNumber".equals(mappedField);

            String slug = isPhone ? "phone_number"
                    : (mappedField != null ? camelToSnake(mappedField) : slugify(header));
            String unique = slug;
            int suffix = 2;
            while (usedSlugs.contains(unique)) {
                unique = slug + "_" + suffix++;
            }
            usedSlugs.add(unique);

            int posIdx = isPhone ? -1 : ++positional;
            columns.add(new DetectedColumn(i, posIdx, header, unique, mappedField, isPhone));
        }

        if (columns.isEmpty()) {
            throw new IllegalArgumentException("Header row is empty — please add column headers in row 1.");
        }
        return columns;
    }

    private Map<String, String> readRow(Row row, List<DetectedColumn> columns, DataFormatter fmt) {
        Map<String, String> values = new HashMap<>();
        for (DetectedColumn col : columns) {
            if (col.getMappedField() == null) continue;
            Cell cell = row.getCell(col.getColumnIndex());
            if (cell == null) continue;
            String v = fmt.formatCellValue(cell).trim();
            if (!v.isEmpty()) values.put(col.getMappedField(), v);
        }
        return values;
    }

    private String validate(Candidate c) {
        if (isBlank(c.getPhoneNumber())) return "Phone Number missing";
        if (!isValidPhone(c.getPhoneNumber()))
            return "Phone Number format invalid: " + c.getPhoneNumber();
        return null;
    }

    private boolean isRowEmpty(Row row, DataFormatter fmt) {
        if (row == null) return true;
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell c = row.getCell(i);
            if (c != null && !fmt.formatCellValue(c).trim().isEmpty()) return false;
        }
        return true;
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    /** "Panel Name" → "panel_name", "Round 1 Time" → "round_1_time" */
    static String slugify(String s) {
        if (s == null) return "";
        String r = s.trim().toLowerCase()
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return r.isEmpty() ? "col" : r;
    }

    /** "candidateName" → "candidate_name" */
    static String camelToSnake(String s) {
        return s.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    /**
     * Normalize a phone number per RD §2.B:
     *   - strip spaces, dashes, parentheses
     *   - convert scientific-notation cells (9.18765E+11) back to plain digits
     *   - if 10 digits → prefix +91
     *   - if starts with 91 and 12 digits total → prefix +
     *   - if already starts with + → keep as-is
     */
    String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.replaceAll("[\\s\\-()]", "");
        if (cleaned.matches("\\d+\\.\\d+E\\+?\\d+")) {
            try { cleaned = new java.math.BigDecimal(cleaned).toPlainString(); } catch (Exception ignored) {}
        }
        if (cleaned.startsWith("+")) return cleaned;
        if (cleaned.length() == 10 && cleaned.matches("\\d{10}")) return "+91" + cleaned;
        if (cleaned.length() == 12 && cleaned.startsWith("91")) return "+" + cleaned;
        if (cleaned.length() == 11 && cleaned.startsWith("0")) return "+91" + cleaned.substring(1);
        return cleaned;
    }

    private boolean isValidPhone(String phone) {
        return phone != null && phone.matches("\\+\\d{10,15}");
    }
}

