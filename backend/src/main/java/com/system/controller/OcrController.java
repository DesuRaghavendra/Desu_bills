package com.system.controller;

import com.system.dto.OcrPreviewResponse;
import com.system.service.OcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR Staging", description = "Endpoints for parsing uploaded spreadsheet images")
public class OcrController {

    private final OcrService ocrService;

    @PostMapping("/preview/new-table")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Upload image and extract tabular preview structure")
    public OcrPreviewResponse previewNewTable(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.system.exception.InvalidImageException("Uploaded file cannot be empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new com.system.exception.InvalidImageException("File size exceeds maximum allowed limit of 10MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches("(?i).*\\.(jpg|jpeg|png)$")) {
            throw new com.system.exception.InvalidImageException("Invalid file extension. Only .jpg, .jpeg, and .png extensions are allowed");
        }

        return ocrService.previewNewTable(file);
    }

    @PostMapping("/preview/existing-table")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Processes an image and matches it against an existing target table schema")
    public com.system.dto.OcrAppendPreviewResponse previewExistingTable(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tableId") java.util.UUID tableId
    ) {
        if (file == null || file.isEmpty()) {
            throw new com.system.exception.InvalidImageException("Uploaded file cannot be empty");
        }
        if (file.getSize() > 10 * 1024 * 1024) {
            throw new com.system.exception.InvalidImageException("File size exceeds maximum allowed limit of 10MB");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.matches("(?i).*\\.(jpg|jpeg|png)$")) {
            throw new com.system.exception.InvalidImageException("Invalid file extension. Only .jpg, .jpeg, and .png extensions are allowed");
        }

        return ocrService.previewExistingTable(file, tableId);
    }
}
