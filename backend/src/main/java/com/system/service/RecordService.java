package com.system.service;

import com.system.dto.RecordResponse;
import com.system.dto.UpdateRecordRequest;
import com.system.entity.Record;
import com.system.entity.TableDefinition;
import com.system.entity.User;
import com.system.exception.ForbiddenException;
import com.system.repository.RecordRepository;
import com.system.repository.TableDefinitionRepository;
import com.system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RecordService {

    private final RecordRepository recordRepository;
    private final TableDefinitionRepository tableDefinitionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * Updates a single record's data after validating against its parent table schema.
     */
    @Transactional
    public RecordResponse updateRecord(UUID recordId, UpdateRecordRequest request) {
        User user = getCurrentUser();
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found with ID: " + recordId));

        if (!record.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this record and cannot modify it.");
        }

        // Fetch the parent table schema for validation
        TableDefinition table = record.getTableDefinition();
        com.system.dto.CreateTableRequest.SchemaDto schema = parseSchema(table.getSchemaJson());
        List<com.system.dto.CreateTableRequest.ColumnDto> columns = schema.getColumns();

        // Validate the incoming data matches the schema
        Map<String, Object> newData = request.getData();
        validateRow(newData, columns);

        // Serialize and persist
        String dataJson;
        try {
            dataJson = objectMapper.writeValueAsString(newData);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize updated record data", e);
        }

        record.setData(dataJson);
        record = recordRepository.save(record);

        log.info("Record {} updated successfully for user {}", recordId, user.getId());

        return toRecordResponse(record);
    }

    /**
     * Deletes a single record after verifying ownership.
     */
    @Transactional
    public void deleteRecord(UUID recordId) {
        User user = getCurrentUser();
        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found with ID: " + recordId));

        if (!record.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this record and cannot delete it.");
        }

        recordRepository.delete(record);
        log.info("Record {} deleted by user {}", recordId, user.getId());
    }

    /**
     * Batch deletes multiple records after verifying all belong to the current user.
     */
    @Transactional
    public void batchDeleteRecords(List<UUID> recordIds) {
        User user = getCurrentUser();

        List<Record> records = recordRepository.findAllByRecordIdIn(recordIds);

        if (records.size() != recordIds.size()) {
            throw new RuntimeException("Some record IDs were not found");
        }

        // Verify all records belong to the current user
        for (Record record : records) {
            if (!record.getUser().getId().equals(user.getId())) {
                throw new ForbiddenException(
                        "You do not own record " + record.getRecordId() + " and cannot delete it.");
            }
        }

        recordRepository.deleteAllByRecordIdIn(recordIds);
        log.info("Batch deleted {} records for user {}", recordIds.size(), user.getId());
    }

    // ---- Private helpers ----

    private void validateRow(Map<String, Object> row,
                             List<com.system.dto.CreateTableRequest.ColumnDto> columns) {
        if (row.size() != columns.size()) {
            throw new IllegalArgumentException(
                    "Row column count (" + row.size() + ") does not match schema column count (" + columns.size() + ")");
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
                        throw new IllegalArgumentException(
                                "Value '" + strVal + "' in column '" + col.getName() + "' is not a valid decimal");
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

    private com.system.dto.CreateTableRequest.SchemaDto parseSchema(String schemaJson) {
        try {
            return objectMapper.readValue(schemaJson,
                    com.system.dto.CreateTableRequest.SchemaDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid table schema", e);
        }
    }

    private RecordResponse toRecordResponse(Record record) {
        Map<String, Object> dataMap;
        try {
            dataMap = objectMapper.readValue(record.getData(), Map.class);
        } catch (Exception e) {
            dataMap = Collections.emptyMap();
        }
        return RecordResponse.builder()
                .recordId(record.getRecordId())
                .data(dataMap)
                .updatedAt(record.getUpdatedAt())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }
}
