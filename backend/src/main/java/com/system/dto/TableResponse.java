package com.system.dto;

import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableResponse {
    private UUID tableId;
    private String tableName;
    private long totalRecords;
    private OffsetDateTime createdAt;
}
