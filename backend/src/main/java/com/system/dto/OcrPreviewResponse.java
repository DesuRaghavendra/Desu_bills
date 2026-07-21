package com.system.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OcrPreviewResponse {
    private List<String> headers;
    private Map<String, String> suggestedTypes;
    private List<List<String>> rows;
}
