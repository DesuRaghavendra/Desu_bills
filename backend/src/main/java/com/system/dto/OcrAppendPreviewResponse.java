package com.system.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrAppendPreviewResponse {
    private List<Map<String, Object>> mappedRows;
    private List<UnmappedColumnDto> unmappedColumns;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UnmappedColumnDto {
        private String ocrColumnName;
        private List<String> sampleValues;
    }
}
