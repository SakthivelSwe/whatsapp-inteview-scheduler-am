package com.dummby.interviewscheduler.model.dto;

import lombok.*;

/**
 * One Excel column detected at upload time, exposed to the UI so HR knows
 * exactly what placeholders are available for templates.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ColumnSchema {
    /** Original Excel header text (e.g. "Panel Name"). */
    private String header;
    /** Slugified, template-safe key (e.g. "panel_name"). */
    private String slug;
    /** {{slug}} — primary recommended placeholder. */
    private String namedPlaceholder;
    /** {{column_N}} — positional placeholder (null for the Phone Number column). */
    private String positionalPlaceholder;
    /** Sample value from the first data row, helps HR understand the column. */
    private String sampleValue;
    /** True if this is the system phone column (not used as a placeholder). */
    private boolean phoneColumn;
    /** True if this maps to a built-in / well-known field. */
    private boolean known;
}

