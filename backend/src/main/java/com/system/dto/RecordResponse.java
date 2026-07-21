package com.system.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordResponse {
    private UUID recordId;
    private Map<String, Object> data;
    private OffsetDateTime updatedAt;
}
