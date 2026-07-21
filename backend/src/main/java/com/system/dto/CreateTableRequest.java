package com.system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTableRequest {

    @NotBlank(message = "Table Name cannot be empty")
    private String tableName;

    @NotNull(message = "Schema cannot be null")
    @Valid
    private SchemaDto schema;

    private List<Map<String, Object>> initialRows;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SchemaDto {
        @NotEmpty(message = "Schema must contain at least one column")
        @Valid
        private List<ColumnDto> columns;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ColumnDto {
        @NotBlank(message = "Column name cannot be blank")
        private String name;

        @NotBlank(message = "Column type cannot be blank")
        private String type; // string, integer, decimal, boolean
    }
}
