package com.system.dto;

import lombok.*;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TableDetailResponse {
    private UUID tableId;
    private String tableName;
    private CreateTableRequest.SchemaDto schema;
}
