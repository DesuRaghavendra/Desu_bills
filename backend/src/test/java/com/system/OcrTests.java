package com.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.system.dto.OcrPreviewResponse;
import com.system.entity.User;
import com.system.entity.TableDefinition;
import com.system.repository.UserRepository;
import com.system.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OcrTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.system.service.OcrClient ocrClient;

    private MockRestServiceServer mockServer;
    private String token;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        User user = User.builder()
                .username("testocr")
                .email("ocr@example.com")
                .passwordHash("password")
                .build();
        userRepository.save(user);

        org.springframework.security.core.userdetails.UserDetails ud = 
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities(java.util.Collections.emptyList())
                        .build();
        token = "Bearer " + jwtService.generateToken(ud);

        mockServer = MockRestServiceServer.createServer(ocrClient.getRestTemplate());
    }

    @Test
    void testPreviewNewTableValidationFailureEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);

        mockMvc.perform(multipart("/api/ocr/preview/new-table")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE"))
                .andExpect(jsonPath("$.message").value("Uploaded file cannot be empty"));
    }

    @Test
    void testPreviewNewTableValidationFailureTooLarge() throws Exception {
        byte[] largeBytes = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", largeBytes);

        mockMvc.perform(multipart("/api/ocr/preview/new-table")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE"))
                .andExpect(jsonPath("$.message").value("File size exceeds maximum allowed limit of 10MB"));
    }

    @Test
    void testPreviewNewTableValidationFailureUnsupportedExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/ocr/preview/new-table")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE"))
                .andExpect(jsonPath("$.message").value("Invalid file extension. Only .jpg, .jpeg, and .png extensions are allowed"));
    }

    @Test
    void testPreviewNewTableSuccess() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "dummy-image-bytes".getBytes());

        Map<String, Object> mockOcrResponse = new HashMap<>();
        mockOcrResponse.put("headers", List.of("Item Code", "Cost", "In Stock"));
        mockOcrResponse.put("rows", List.of(
                List.of("A102", "299.50", "true"),
                List.of("B504", "19.99", "false")
        ));

        mockServer.expect(requestTo("http://localhost:8000/api/v1/ocr/process"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockOcrResponse), MediaType.APPLICATION_JSON));

        mockMvc.perform(multipart("/api/ocr/preview/new-table")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.headers[0]").value("Item Code"))
                .andExpect(jsonPath("$.suggestedTypes.['Item Code']").value("string"))
                .andExpect(jsonPath("$.suggestedTypes.Cost").value("decimal"))
                .andExpect(jsonPath("$.suggestedTypes.['In Stock']").value("string"))
                .andExpect(jsonPath("$.rows[0][0]").value("A102"))
                .andExpect(jsonPath("$.rows[0][1]").value("299.50"));

        mockServer.verify();
    }

    @Test
    void testPreviewNewTableOcrConnectionFailure() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "dummy-image-bytes".getBytes());

        mockServer.expect(requestTo("http://localhost:8000/api/v1/ocr/process"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withServerError());

        mockMvc.perform(multipart("/api/ocr/preview/new-table")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value("OCR_PROCESSING_FAILURE"));

        mockServer.verify();
    }

    @Autowired
    private com.system.repository.TableDefinitionRepository tableDefinitionRepository;

    @Test
    void testPreviewExistingTableSuccess() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(userRepository.findAll().get(0))
                .tableName("Existing Target Table")
                .schemaJson("{\"columns\": [{\"name\": \"Item Code\", \"type\": \"string\"}, {\"name\": \"Cost\", \"type\": \"decimal\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", "dummy-image-bytes".getBytes());

        Map<String, Object> mockOcrResponse = new HashMap<>();
        mockOcrResponse.put("headers", List.of("item_code ", "price"));
        mockOcrResponse.put("rows", List.of(
                List.of("C901", "12.00")
        ));

        mockServer.expect(requestTo("http://localhost:8000/api/v1/ocr/process"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(objectMapper.writeValueAsString(mockOcrResponse), MediaType.APPLICATION_JSON));

        mockMvc.perform(multipart("/api/ocr/preview/existing-table")
                        .file(file)
                        .param("tableId", table.getTableId().toString())
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mappedRows[0]['Item Code']").value("C901"))
                .andExpect(jsonPath("$.mappedRows[0].price").value("12.00"))
                .andExpect(jsonPath("$.unmappedColumns[0].ocrColumnName").value("price"))
                .andExpect(jsonPath("$.unmappedColumns[0].sampleValues[0]").value("12.00"));

        mockServer.verify();
    }
}
