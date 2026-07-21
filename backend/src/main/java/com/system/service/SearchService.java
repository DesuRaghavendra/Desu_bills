package com.system.service;

import com.system.dto.PageResponse;
import com.system.dto.RecordResponse;
import com.system.dto.SearchRequest;
import com.system.dto.SearchFilterDto;
import com.system.entity.Record;
import com.system.entity.TableDefinition;
import com.system.entity.User;
import com.system.exception.ForbiddenException;
import com.system.repository.TableDefinitionRepository;
import com.system.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
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
public class SearchService {

    private final TableDefinitionRepository tableDefinitionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Allowed characters for column names to prevent SQL injection when
     * embedding them directly inside the JSONB ->> operator.
     */
    private static final java.util.regex.Pattern SAFE_COLUMN_NAME =
            java.util.regex.Pattern.compile("^[\\w\\s\\-\\.]+$");

    public PageResponse<RecordResponse> search(UUID tableId, SearchRequest request) {
        User user = getCurrentUser();
        TableDefinition table = tableDefinitionRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + tableId));

        if (!table.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this table and cannot search it.");
        }

        // Parse schema to get column types
        com.system.dto.CreateTableRequest.SchemaDto schema;
        try {
            schema = objectMapper.readValue(table.getSchemaJson(),
                    com.system.dto.CreateTableRequest.SchemaDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid table schema", e);
        }

        Map<String, String> columnTypeMap = schema.getColumns().stream()
                .collect(Collectors.toMap(
                        com.system.dto.CreateTableRequest.ColumnDto::getName,
                        col -> col.getType().toLowerCase()));

        // Build filter WHERE clauses
        StringBuilder filterSql = new StringBuilder();
        Map<String, Object> filterParams = new HashMap<>();

        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            int idx = 0;
            for (SearchFilterDto filter : request.getFilters()) {
                String col = filter.getColumn();
                String op = filter.getOperator();
                Object val = filter.getValue();
                String type;
                if ("DATE".equalsIgnoreCase(col) || "date".equalsIgnoreCase(col) || "updated_at".equalsIgnoreCase(col)) {
                    type = "date";
                } else {
                    type = columnTypeMap.get(col);
                }

                if (type == null) {
                    throw new IllegalArgumentException(
                            "Column '" + col + "' does not exist in schema");
                }

                // Validate column name against safe pattern to prevent SQL injection
                if (!SAFE_COLUMN_NAME.matcher(col).matches()) {
                    throw new IllegalArgumentException(
                            "Column name '" + col + "' contains invalid characters");
                }

                String paramName = "val" + idx;
                String escapedCol = col.replace("'", "''");

                switch (type) {
                    case "string":
                        buildStringFilter(filterSql, escapedCol, op, val, paramName, filterParams);
                        break;
                    case "integer":
                    case "decimal":
                        buildNumericFilter(filterSql, escapedCol, op, val, filter.getMaxValue(),
                                paramName, idx, filterParams);
                        break;
                    case "date":
                        buildDateFilter(filterSql, op, val, filter.getMaxValue(), paramName, idx, filterParams);
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported column type: " + type);
                }
                idx++;
            }
        }

        // Base query
        String baseSql = "SELECT r.* FROM records r WHERE r.user_id = :userId AND r.table_id = :tableId"
                + filterSql;
        String countSql = "SELECT COUNT(*) FROM records r WHERE r.user_id = :userId AND r.table_id = :tableId"
                + filterSql;

        Map<String, Object> baseParams = new HashMap<>();
        baseParams.put("userId", user.getId());
        baseParams.put("tableId", tableId);
        baseParams.putAll(filterParams);

        // Pagination
        int limit = request.getSize();
        int offset = request.getPage() * limit;

        String dataSql = baseSql + " ORDER BY r.created_at DESC LIMIT :limit OFFSET :offset";
        baseParams.put("limit", limit);
        baseParams.put("offset", offset);

        // Execute data query
        Query dataQuery = entityManager.createNativeQuery(dataSql, Record.class);
        baseParams.forEach(dataQuery::setParameter);

        @SuppressWarnings("unchecked")
        List<Record> records = dataQuery.getResultList();

        List<RecordResponse> content = records.stream()
                .map(this::toRecordResponse)
                .collect(Collectors.toList());

        // Execute count query (without limit/offset params)
        Map<String, Object> countParams = new HashMap<>(baseParams);
        countParams.remove("limit");
        countParams.remove("offset");

        Query countQuery = entityManager.createNativeQuery(countSql);
        countParams.forEach(countQuery::setParameter);
        Number total = (Number) countQuery.getSingleResult();

        int totalPages = limit > 0 ? (int) Math.ceil((double) total.longValue() / limit) : 0;

        log.info("Search executed on table {} with {} filters, returned {} of {} total results",
                tableId, request.getFilters() != null ? request.getFilters().size() : 0,
                content.size(), total.longValue());

        return PageResponse.<RecordResponse>builder()
                .content(content)
                .totalPages(totalPages)
                .totalElements(total.longValue())
                .build();
    }

    private void buildStringFilter(StringBuilder sql, String col, String op, Object val,
                                   String paramName, Map<String, Object> params) {
        switch (op.toLowerCase()) {
            case "contains":
                sql.append(" AND r.data ->> '").append(col).append("' ILIKE :").append(paramName);
                params.put(paramName, "%" + val.toString() + "%");
                break;
            case "startswith":
                sql.append(" AND r.data ->> '").append(col).append("' ILIKE :").append(paramName);
                params.put(paramName, val.toString() + "%");
                break;
            case "equals":
                sql.append(" AND r.data ->> '").append(col).append("' = :").append(paramName);
                params.put(paramName, val.toString());
                break;
            default:
                throw new IllegalArgumentException("Unsupported string operator: " + op);
        }
    }

    private void buildNumericFilter(StringBuilder sql, String col, String op, Object val,
                                    Object maxVal, String paramName, int idx,
                                    Map<String, Object> params) {
        String castExpr = "(r.data ->> '" + col + "')::numeric";

        switch (op.toLowerCase()) {
            case "equals":
                sql.append(" AND ").append(castExpr).append(" = :").append(paramName);
                params.put(paramName, toDouble(val));
                break;
            case "greaterthan":
                sql.append(" AND ").append(castExpr).append(" > :").append(paramName);
                params.put(paramName, toDouble(val));
                break;
            case "greaterthanorequal":
                sql.append(" AND ").append(castExpr).append(" >= :").append(paramName);
                params.put(paramName, toDouble(val));
                break;
            case "lessthan":
                sql.append(" AND ").append(castExpr).append(" < :").append(paramName);
                params.put(paramName, toDouble(val));
                break;
            case "lessthanorequal":
                sql.append(" AND ").append(castExpr).append(" <= :").append(paramName);
                params.put(paramName, toDouble(val));
                break;
            case "between":
                if (maxVal == null) {
                    throw new IllegalArgumentException(
                            "Between operator requires a maxValue parameter");
                }
                String paramMax = "max" + idx;
                sql.append(" AND ").append(castExpr).append(" BETWEEN :").append(paramName)
                        .append(" AND :").append(paramMax);
                params.put(paramName, toDouble(val));
                params.put(paramMax, toDouble(maxVal));
                break;
            default:
                throw new IllegalArgumentException("Unsupported numeric operator: " + op);
        }
    }

    private void buildDateFilter(StringBuilder sql, String op, Object val, Object maxVal,
                                 String paramName, int idx, Map<String, Object> params) {
        String dateExpr = "CAST(r.updated_at AS date)";
        String todayStr = java.time.LocalDate.now().toString();

        switch (op.toLowerCase()) {
            case "equals":
                sql.append(" AND ").append(dateExpr).append(" = CAST(:").append(paramName).append(" AS date)");
                params.put(paramName, sanitizeDate(val, todayStr));
                break;
            case "greaterthan":
            case "greaterthanorequal":
                sql.append(" AND ").append(dateExpr).append(" >= CAST(:").append(paramName).append(" AS date)");
                params.put(paramName, sanitizeDate(val, todayStr));
                break;
            case "lessthan":
            case "lessthanorequal":
                sql.append(" AND ").append(dateExpr).append(" <= CAST(:").append(paramName).append(" AS date)");
                params.put(paramName, sanitizeDate(val, todayStr));
                break;
            case "between":
                if (maxVal == null || maxVal.toString().trim().isEmpty()) {
                    throw new IllegalArgumentException("Between operator requires a maxValue parameter for Date range");
                }
                String paramMax = "max" + idx;
                sql.append(" AND ").append(dateExpr).append(" BETWEEN CAST(:").append(paramName).append(" AS date) AND CAST(:").append(paramMax).append(" AS date)");
                params.put(paramName, sanitizeDate(val, todayStr));
                params.put(paramMax, sanitizeDate(maxVal, todayStr));
                break;
            default:
                throw new IllegalArgumentException("Unsupported date operator: " + op);
        }
    }

    private String sanitizeDate(Object val, String todayStr) {
        if (val == null || val.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("Date value cannot be blank");
        }
        String dateStr = val.toString().trim();
        if (dateStr.contains("T")) {
            dateStr = dateStr.substring(0, dateStr.indexOf("T"));
        }
        if (dateStr.compareTo(todayStr) > 0) {
            dateStr = todayStr;
        }
        return dateStr;
    }

    private double toDouble(Object val) {
        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Value '" + val + "' is not a valid number");
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
