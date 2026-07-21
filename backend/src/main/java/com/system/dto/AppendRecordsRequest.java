package com.system.dto;

import lombok.*;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppendRecordsRequest {
    @NotEmpty(message = "Records list cannot be empty")
    private List<Map<String, String>> records;
}
