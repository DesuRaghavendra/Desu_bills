package com.system;

import com.system.entity.TableDefinition;
import com.system.entity.User;
import com.system.repository.TableDefinitionRepository;
import com.system.repository.UserRepository;
import com.system.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ErrorHandlingTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableDefinitionRepository tableDefinitionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private User user;

    @BeforeEach
    void setUp() {
        tableDefinitionRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .username("erruser")
                .email("erruser@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .build();
        user = userRepository.save(user);

        org.springframework.security.core.userdetails.UserDetails ud =
                org.springframework.security.core.userdetails.User.builder()
                        .username(user.getEmail())
                        .password(user.getPasswordHash())
                        .authorities(java.util.Collections.emptyList())
                        .build();

        token = "Bearer " + jwtService.generateToken(ud);
    }

    @Test
    void testDuplicateTableNameError() throws Exception {
        TableDefinition existing = TableDefinition.builder()
                .user(user)
                .tableName("Existing Table")
                .schemaJson("{\"columns\": [{\"name\": \"Item\", \"type\": \"string\"}]}")
                .build();
        tableDefinitionRepository.save(existing);

        String payload = """
                {
                  "tableName": "Existing Table",
                  "schema": {
                    "columns": [ { "name": "Item", "type": "string" } ]
                  },
                  "initialRows": []
                }
                """;

        mockMvc.perform(post("/api/tables")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_TABLE_NAME"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testInvalidImageFileExtensionError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "hello text".getBytes()
        );

        mockMvc.perform(multipart("/api/ocr/preview/new-table")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE"))
                .andExpect(jsonPath("$.message").value("Invalid file extension. Only .jpg, .jpeg, and .png extensions are allowed"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void testEmptyImageFileError() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "empty.jpg",
                "image/jpeg",
                new byte[0]
        );

        mockMvc.perform(multipart("/api/ocr/preview/new-table")
                        .file(file)
                        .header("Authorization", token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_IMAGE"))
                .andExpect(jsonPath("$.message").value("Uploaded file cannot be empty"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
