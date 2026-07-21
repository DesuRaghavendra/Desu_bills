package com.system.service;

import com.system.dto.OcrPreviewResponse;
import com.system.exception.OcrProcessingException;
import com.system.exception.ForbiddenException;
import com.system.entity.TableDefinition;
import com.system.entity.User;
import com.system.repository.TableDefinitionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OcrService {

    private final OcrClient ocrClient;
    private final TableDefinitionRepository tableDefinitionRepository;
    private final TableService tableService;
    private final ObjectMapper objectMapper;

    public OcrPreviewResponse previewNewTable(MultipartFile file) {
        Map<String, Object> ocrResponse = ocrClient.processImage(file);
        
        if (ocrResponse == null) {
            throw new OcrProcessingException("OCR microservice returned empty response");
        }

        List<String> headers;
        try {
            headers = (List<String>) ocrResponse.get("headers");
        } catch (Exception e) {
            throw new OcrProcessingException("Failed to parse headers from OCR response", e);
        }

        List<List<String>> rows;
        try {
            rows = (List<List<String>>) ocrResponse.get("rows");
        } catch (Exception e) {
            throw new OcrProcessingException("Failed to parse rows from OCR response", e);
        }

        if (headers == null) {
            headers = Collections.emptyList();
        }
        if (rows == null) {
            rows = Collections.emptyList();
        }

        Map<String, String> suggestedTypes = new LinkedHashMap<>();
        for (int j = 0; j < headers.size(); j++) {
            String header = headers.get(j);
            List<String> colValues = new ArrayList<>();
            for (List<String> row : rows) {
                if (row != null && j < row.size()) {
                    colValues.add(row.get(j));
                }
            }
            suggestedTypes.put(header, inferType(colValues));
        }

        return OcrPreviewResponse.builder()
                .headers(headers)
                .suggestedTypes(suggestedTypes)
                .rows(rows)
                .build();
    }

    private String inferType(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "string";
        }

        boolean allDecimal = true;
        int nonOptCount = 0;

        for (String val : values) {
            if (val == null || val.trim().isEmpty()) {
                continue;
            }
            nonOptCount++;
            String clean = val.trim();

            if (!clean.matches("-?\\d+(\\.\\d+)?")) {
                allDecimal = false;
            }
        }

        if (nonOptCount > 0 && allDecimal) {
            return "decimal";
        }
        return "string";
    }

    public com.system.dto.OcrAppendPreviewResponse previewExistingTable(MultipartFile file, UUID tableId) {
        User user = tableService.getCurrentUser();
        TableDefinition table = tableDefinitionRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found with ID: " + tableId));

        if (!table.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You do not own this table.");
        }

        com.system.dto.CreateTableRequest.SchemaDto schemaDto;
        try {
            schemaDto = objectMapper.readValue(table.getSchemaJson(), com.system.dto.CreateTableRequest.SchemaDto.class);
        } catch (Exception e) {
            throw new RuntimeException("Invalid schema in table definition");
        }

        List<String> targetColumns = schemaDto.getColumns().stream()
                .map(com.system.dto.CreateTableRequest.ColumnDto::getName)
                .collect(Collectors.toList());

        Map<String, Object> ocrResponse = ocrClient.processImage(file);
        if (ocrResponse == null) {
            throw new OcrProcessingException("OCR microservice returned empty response");
        }

        List<String> ocrHeaders;
        try {
            ocrHeaders = (List<String>) ocrResponse.get("headers");
        } catch (Exception e) {
            throw new OcrProcessingException("Failed to parse headers from OCR response", e);
        }

        List<List<String>> ocrRows;
        try {
            ocrRows = (List<List<String>>) ocrResponse.get("rows");
        } catch (Exception e) {
            throw new OcrProcessingException("Failed to parse rows from OCR response", e);
        }

        if (ocrHeaders == null) {
            ocrHeaders = Collections.emptyList();
        }
        if (ocrRows == null) {
            ocrRows = Collections.emptyList();
        }

        Map<String, String> normalizedTargetToActual = targetColumns.stream()
                .collect(Collectors.toMap(
                        this::normalizeHeader,
                        col -> col,
                        (existing, replacement) -> existing
                ));

        Map<String, String> ocrToTargetColumn = new HashMap<>();
        List<String> unmappedOcrHeaders = new ArrayList<>();

        for (String ocrHeader : ocrHeaders) {
            String normOcr = normalizeHeader(ocrHeader);
            if (normalizedTargetToActual.containsKey(normOcr)) {
                ocrToTargetColumn.put(ocrHeader, normalizedTargetToActual.get(normOcr));
            } else {
                unmappedOcrHeaders.add(ocrHeader);
            }
        }

        List<Map<String, Object>> mappedRows = new ArrayList<>();
        for (List<String> row : ocrRows) {
            Map<String, Object> mappedRow = new HashMap<>();
            for (String targetCol : targetColumns) {
                mappedRow.put(targetCol, null);
            }
            for (int i = 0; i < ocrHeaders.size(); i++) {
                if (i < row.size()) {
                    String ocrHeader = ocrHeaders.get(i);
                    String val = row.get(i);
                    if (ocrToTargetColumn.containsKey(ocrHeader)) {
                        String targetCol = ocrToTargetColumn.get(ocrHeader);
                        mappedRow.put(targetCol, val);
                    } else {
                        mappedRow.put(ocrHeader, val);
                    }
                }
            }
            mappedRows.add(mappedRow);
        }

        List<com.system.dto.OcrAppendPreviewResponse.UnmappedColumnDto> unmappedColumns = new ArrayList<>();
        for (String unmappedHeader : unmappedOcrHeaders) {
            List<String> sampleValues = new ArrayList<>();
            int colIdx = ocrHeaders.indexOf(unmappedHeader);
            if (colIdx >= 0) {
                for (List<String> row : ocrRows) {
                    if (colIdx < row.size()) {
                        String val = row.get(colIdx);
                        if (val != null && !val.trim().isEmpty()) {
                            sampleValues.add(val);
                            if (sampleValues.size() >= 3) break;
                        }
                    }
                }
            }
            unmappedColumns.add(com.system.dto.OcrAppendPreviewResponse.UnmappedColumnDto.builder()
                    .ocrColumnName(unmappedHeader)
                    .sampleValues(sampleValues)
                    .build());
        }

        return com.system.dto.OcrAppendPreviewResponse.builder()
                .mappedRows(mappedRows)
                .unmappedColumns(unmappedColumns)
                .build();
    }

    private String normalizeHeader(String header) {
        if (header == null) return "";
        return header.toLowerCase().replaceAll("[\\s_]+", "");
    }
}
