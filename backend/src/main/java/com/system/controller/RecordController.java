package com.system.controller;

import com.system.dto.BatchDeleteRequest;
import com.system.dto.RecordResponse;
import com.system.dto.UpdateRecordRequest;
import com.system.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@Tag(name = "Record Management", description = "Endpoints for modifying and deleting individual data rows")
public class RecordController {

    private final RecordService recordService;

    @PutMapping("/{recordId}")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Modifies field-level values within a specific saved data row")
    public RecordResponse updateRecord(
            @PathVariable UUID recordId,
            @Valid @RequestBody UpdateRecordRequest request
    ) {
        return recordService.updateRecord(recordId, request);
    }

    @DeleteMapping("/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Deletes a specific data row")
    public void deleteRecord(@PathVariable UUID recordId) {
        recordService.deleteRecord(recordId);
    }

    @DeleteMapping("/batch")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Batch deletes multiple data rows by record IDs")
    public void batchDeleteRecords(@Valid @RequestBody BatchDeleteRequest request) {
        recordService.batchDeleteRecords(request.getRecordIds());
    }
}
