package com.system.service;

import com.system.dto.TableResponse;
import com.system.entity.TableDefinition;
import com.system.entity.User;
import com.system.exception.ForbiddenException;
import com.system.repository.RecordRepository;
import com.system.repository.TableDefinitionRepository;
import com.system.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TableService {

    private final TableDefinitionRepository tableDefinitionRepository;
    private final RecordRepository recordRepository;
    private final UserRepository userRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @Transactional
    public com.system.dto.CreateTableResponse createTable(com.system.dto.CreateTableRequest request) {
        User user = getCurrentUser();

        // 1. Check duplicate table name for this user
        if (tableDefinitionRepository.findByUser_IdAndTableName(user.getId(), request.getTableName()).isPresent()) {
            throw new com.system.exception.DuplicateTableNameException("A table with name '" + request.getTableName() + "' already exists in your workspace.");
        }

        // 2. Validate columns
        List<com.system.dto.CreateTableRequest.ColumnDto> columns = request.getSchema().getColumns();
        java.util.Set<String> colNames = new java.util.LinkedHashSet<>();
        for (com.system.dto.CreateTableRequest.ColumnDto col : columns) {
            if (col.getName() == null || col.getName().trim().isEmpty()) {
                throw new IllegalArgumentException("Column name cannot be blank");
            }
            if (col.getType() == null || col.getType().trim().isEmpty()) {
                throw new IllegalArgumentException("Column type cannot be blank");
            }
            String type = col.getType().toLowerCase();
            if (!List.of("string", "decimal").contains(type)) {
                throw new IllegalArgumentException("Invalid column type: " + col.getType());
            }
            if (!colNames.add(col.getName())) {
                throw new IllegalArgumentException("Duplicate column name: " + col.getName());
            }
        }

        // 3. Validate rows
        List<java.util.Map<String, Object>> rows = request.getInitialRows();
        if (rows != null) {
            for (java.util.Map<String, Object> row : rows) {
                validateRow(row, columns);
            }
        }

        // 4. Save TableDefinition
        String schemaJson;
        try {
            schemaJson = objectMapper.writeValueAsString(request.getSchema());
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize schema to JSON", e);
        }

        TableDefinition table = TableDefinition.builder()
                .user(user)
                .tableName(request.getTableName())
                .schemaJson(schemaJson)
                .build();
        table = tableDefinitionRepository.save(table);

        // 5. Save Records
        if (rows != null) {
            for (java.util.Map<String, Object> row : rows) {
                String rowDataJson;
                try {
                    rowDataJson = objectMapper.writeValueAsString(row);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to serialize row data to JSON", e);
                }

                com.system.entity.Record record = com.system.entity.Record.builder()
                        .user(user)
                        .tableDefinition(table)
                        .data(rowDataJson)
                        .build();
                recordRepository.save(record);
            }
        }

        return com.system.dto.CreateTableResponse.builder()
                .tableId(table.getTableId())
                .tableName(table.getTableName())
                .build();
    }

    private void validateRow(java.util.Map<String, Object> row, List<com.system.dto.CreateTableRequest.ColumnDto> columns) {
        if (row.size() != columns.size()) {
            throw new IllegalArgumentException("Row column count does not match schema column count");
        }

        for (com.system.dto.CreateTableRequest.ColumnDto col : columns) {
            if (!row.containsKey(col.getName())) {
                throw new IllegalArgumentException("Row is missing column: " + col.getName());
            }

            Object val = row.get(col.getName());
            if (val == null) {
                continue;
            }

            String strVal = val.toString().trim();
            String type = col.getType().toLowerCase();

            switch (type) {
                case "decimal":
                case "integer":
                    if (!strVal.matches("-?\\d+(\\.\\d+)?")) {
                        throw new IllegalArgumentException("Value '" + strVal + "' in column '" + col.getName() + "' is not a valid decimal");
                    }
                    break;
                case "string":
                case "boolean":
                    break;
                default:
                    throw new IllegalArgumentException("Unknown column type: " + col.getType());
            }
        }
    }

    public User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    public List<TableResponse> getTables() {
        User user = getCurrentUser();
        List<TableDefinition> tables = tableDefinitionRepository.findByUser_Id(user.getId());
        return tables.stream()
                .map(table -> TableResponse.builder()
                        .tableId(table.getTableId())
                        .tableName(table.getTableName())
                        .totalRecords(recordRepository.countByTableDefinition(table))
                        .createdAt(table.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteTable(UUID tableId) {
        User user = getCurrentUser();
        TableDefinition table = tableDefinitionRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + tableId));

        if (!table.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this table and cannot delete it.");
        }

        recordRepository.deleteByTableDefinition(table);
        tableDefinitionRepository.delete(table);
    }

    public com.system.dto.TableDetailResponse getTableDetail(UUID tableId) {
        User user = getCurrentUser();
        TableDefinition table = tableDefinitionRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + tableId));

        if (!table.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this table and cannot view it.");
        }

        com.system.dto.CreateTableRequest.SchemaDto schema;
        try {
            schema = objectMapper.readValue(table.getSchemaJson(), com.system.dto.CreateTableRequest.SchemaDto.class);
        } catch (Exception e) {
            schema = com.system.dto.CreateTableRequest.SchemaDto.builder()
                    .columns(java.util.Collections.emptyList())
                    .build();
        }

        return com.system.dto.TableDetailResponse.builder()
                .tableId(table.getTableId())
                .tableName(table.getTableName())
                .schema(schema)
                .build();
    }

    public com.system.dto.PageResponse<com.system.dto.RecordResponse> getTableRecords(UUID tableId, int page, int size) {
        User user = getCurrentUser();
        TableDefinition table = tableDefinitionRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + tableId));

        if (!table.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this table and cannot view its records.");
        }

        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                page, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );

        org.springframework.data.domain.Page<com.system.entity.Record> recordsPage = 
                recordRepository.findByTableDefinitionAndUser(table, user, pageable);

        java.util.List<com.system.dto.RecordResponse> content = recordsPage.getContent().stream()
                .map(this::toRecordResponse)
                .collect(Collectors.toList());

        return com.system.dto.PageResponse.<com.system.dto.RecordResponse>builder()
                .content(content)
                .totalPages(recordsPage.getTotalPages())
                .totalElements(recordsPage.getTotalElements())
                .build();
    }

    private com.system.dto.RecordResponse toRecordResponse(com.system.entity.Record record) {
        java.util.Map<String, Object> dataMap;
        try {
            dataMap = objectMapper.readValue(record.getData(), java.util.Map.class);
        } catch (Exception e) {
            dataMap = java.util.Collections.emptyMap();
        }
        return com.system.dto.RecordResponse.builder()
                .recordId(record.getRecordId())
                .data(dataMap)
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    @Transactional
    public void appendRecords(UUID tableId, com.system.dto.AppendRecordsRequest request) {
        User user = getCurrentUser();
        TableDefinition table = tableDefinitionRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + tableId));

        if (!table.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this table.");
        }

        com.system.dto.CreateTableRequest.SchemaDto schema;
        try {
            schema = objectMapper.readValue(table.getSchemaJson(), com.system.dto.CreateTableRequest.SchemaDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid table schema");
        }

        for (java.util.Map<String, String> rowMap : request.getRecords()) {
            java.util.Map<String, Object> validateMap = new java.util.HashMap<>(rowMap);
            validateRow(validateMap, schema.getColumns());
        }

        for (java.util.Map<String, String> rowMap : request.getRecords()) {
            String recordDataJson;
            try {
                java.util.Map<String, String> filteredRow = new java.util.HashMap<>();
                for (com.system.dto.CreateTableRequest.ColumnDto col : schema.getColumns()) {
                    filteredRow.put(col.getName(), rowMap.get(col.getName()));
                }
                recordDataJson = objectMapper.writeValueAsString(filteredRow);
            } catch (Exception e) {
                throw new RuntimeException("Serialization failure during record append");
            }

            com.system.entity.Record record = com.system.entity.Record.builder()
                    .user(user)
                    .tableDefinition(table)
                    .data(recordDataJson)
                    .build();
            recordRepository.save(record);
        }
    }
}
