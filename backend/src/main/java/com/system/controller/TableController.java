package com.system.controller;

import com.system.dto.TableResponse;
import com.system.service.TableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.system.dto.SearchRequest;
import com.system.service.SearchService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tables")
@RequiredArgsConstructor
@Tag(name = "Table Management", description = "Endpoints for managing custom logical table structures")
public class TableController {

    private final TableService tableService;
    private final SearchService searchService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Get all dynamic tables owned by the authenticated user")
    public List<TableResponse> getTables() {
        return tableService.getTables();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete a specific table and all its records cascadingly")
    public void deleteTable(@PathVariable UUID id) {
        tableService.deleteTable(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Persist a newly constructed table structure alongside initial staging rows")
    public com.system.dto.CreateTableResponse createTable(@jakarta.validation.Valid @RequestBody com.system.dto.CreateTableRequest request) {
        return tableService.createTable(request);
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Fetch structural details and columns schema for a specific table")
    public com.system.dto.TableDetailResponse getTableDetail(@PathVariable UUID id) {
        return tableService.getTableDetail(id);
    }

    @GetMapping("/{id}/records")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Paginated retrieval of saved records")
    public com.system.dto.PageResponse<com.system.dto.RecordResponse> getTableRecords(
            @PathVariable UUID id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        return tableService.getTableRecords(id, page, size);
    }

    @PostMapping("/{id}/records")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Manually appends batch record instances directly into an existing matrix")
    public void appendRecords(
            @PathVariable UUID id,
            @jakarta.validation.Valid @RequestBody com.system.dto.AppendRecordsRequest request
    ) {
        tableService.appendRecords(id, request);
    }

    @PostMapping("/{id}/search")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Dynamic search on table records")
    public com.system.dto.PageResponse<com.system.dto.RecordResponse> search(@PathVariable UUID id, @jakarta.validation.Valid @RequestBody SearchRequest request) {
        return searchService.search(id, request);
    }
}

