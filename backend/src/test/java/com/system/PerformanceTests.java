package com.system;

import com.system.entity.Record;
import com.system.entity.TableDefinition;
import com.system.entity.User;
import com.system.repository.RecordRepository;
import com.system.repository.TableDefinitionRepository;
import com.system.repository.UserRepository;
import com.system.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class PerformanceTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableDefinitionRepository tableDefinitionRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String token;
    private User user;

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        tableDefinitionRepository.deleteAll();
        userRepository.deleteAll();

        user = User.builder()
                .username("perfuser")
                .email("perfuser@example.com")
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
    void testBatchInsertPerformance() {
        TableDefinition table = TableDefinition.builder()
                .user(user)
                .tableName("Batch Insert Table")
                .schemaJson("{\"columns\": [{\"name\": \"Code\", \"type\": \"string\"}, {\"name\": \"Price\", \"type\": \"decimal\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        List<Record> batchRecords = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            batchRecords.add(Record.builder()
                    .user(user)
                    .tableDefinition(table)
                    .data(String.format("{\"Code\": \"ITEM_%d\", \"Price\": \"%d.99\"}", i, i))
                    .build());
        }

        long t0 = System.currentTimeMillis();
        recordRepository.saveAll(batchRecords);
        long elapsed = System.currentTimeMillis() - t0;

        assertThat(recordRepository.count()).isEqualTo(200);
        assertThat(elapsed).isLessThan(2000);
    }

    @Test
    void testDateSearchExecutionPerformance() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user)
                .tableName("Search Perf Table")
                .schemaJson("{\"columns\": [{\"name\": \"Category\", \"type\": \"string\"}, {\"name\": \"Value\", \"type\": \"decimal\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        List<Record> batchRecords = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            batchRecords.add(Record.builder()
                    .user(user)
                    .tableDefinition(table)
                    .data(String.format("{\"Category\": \"CAT_%d\", \"Value\": \"%d.50\"}", i % 5, i))
                    .build());
        }
        recordRepository.saveAll(batchRecords);

        String today = LocalDate.now().toString();
        String payload = String.format("""
                {
                  "filters": [
                    { "column": "DATE", "operator": "Between", "value": "2020-01-01", "maxValue": "%s" }
                  ],
                  "page": 0,
                  "size": 50
                }
                """, today);

        long t0 = System.currentTimeMillis();
        mockMvc.perform(post("/api/tables/" + table.getTableId() + "/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(50));
        long elapsed = System.currentTimeMillis() - t0;

        assertThat(elapsed).isLessThan(2000);
    }
}
