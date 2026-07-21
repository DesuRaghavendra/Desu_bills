package com.system.service;

import com.system.exception.OcrProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@lombok.Getter
@Slf4j
public class OcrClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${application.ocr.service-url:http://localhost:8000}")
    private String ocrServiceUrl;

    public Map<String, Object> processImage(MultipartFile file) {
        String url = ocrServiceUrl + "/api/v1/ocr/process";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            ByteArrayResource fileResource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return file.getOriginalFilename();
                }
            };
            body.add("file", fileResource);
        } catch (Exception e) {
            throw new OcrProcessingException("Failed to read uploaded file bytes", e);
        }

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            log.info("Forwarding OCR request to microservice url: {}", url);
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);
            return (Map<String, Object>) response.getBody();
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("OCR microservice unreachable or timed out", e);
            throw new com.system.exception.ServiceUnavailableException("OCR microservice is currently unreachable or timed out.", e);
        } catch (Exception e) {
            log.error("OCR microservice communication failure", e);
            throw new OcrProcessingException("Communication with OCR engine failed: " + e.getMessage(), e);
        }
    }
}
