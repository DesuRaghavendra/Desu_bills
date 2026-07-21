package com.system;

import com.system.entity.User;
import com.system.entity.TableDefinition;
import com.system.entity.Record;
import com.system.repository.UserRepository;
import com.system.repository.TableDefinitionRepository;
import com.system.repository.RecordRepository;
import com.system.config.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TableTests {

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
    private jakarta.persistence.EntityManager entityManager;

    private User user1;
    private User user2;
    private String token1;
    private String token2;

    @BeforeEach
    void setUp() {
        recordRepository.deleteAll();
        tableDefinitionRepository.deleteAll();
        userRepository.deleteAll();

        user1 = User.builder()
                .username("user1")
                .email("user1@example.com")
                .passwordHash("password")
                .build();
        user1 = userRepository.save(user1);
        org.springframework.security.core.userdetails.UserDetails ud1 = 
                org.springframework.security.core.userdetails.User.builder()
                        .username(user1.getEmail())
                        .password(user1.getPasswordHash())
                        .authorities(java.util.Collections.emptyList())
                        .build();
        token1 = "Bearer " + jwtService.generateToken(ud1);

        user2 = User.builder()
                .username("user2")
                .email("user2@example.com")
                .passwordHash("password")
                .build();
        user2 = userRepository.save(user2);
        org.springframework.security.core.userdetails.UserDetails ud2 = 
                org.springframework.security.core.userdetails.User.builder()
                        .username(user2.getEmail())
                        .password(user2.getPasswordHash())
                        .authorities(java.util.Collections.emptyList())
                        .build();
        token2 = "Bearer " + jwtService.generateToken(ud2);
    }

    @Test
    void testGetTablesRetrievesUserOwnedOnly() throws Exception {
        TableDefinition table1 = TableDefinition.builder()
                .user(user1)
                .tableName("Table 1")
                .schemaJson("{\"columns\": []}")
                .build();
        tableDefinitionRepository.save(table1);

        TableDefinition table2 = TableDefinition.builder()
                .user(user2)
                .tableName("Table 2")
                .schemaJson("{\"columns\": []}")
                .build();
        tableDefinitionRepository.save(table2);

        mockMvc.perform(get("/api/tables")
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].tableName").value("Table 1"));
    }

    @Test
    void testDeleteTableCascadesAndPurgesRecords() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user1)
                .tableName("User1 Table")
                .schemaJson("{\"columns\": []}")
                .build();
        table = tableDefinitionRepository.save(table);

        Record record = Record.builder()
                .user(user1)
                .tableDefinition(table)
                .data("{}")
                .build();
        recordRepository.save(record);

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(delete("/api/tables/" + table.getTableId())
                        .header("Authorization", token1))
                .andExpect(status().isNoContent());

        assertThat(tableDefinitionRepository.findById(table.getTableId())).isEmpty();
        assertThat(recordRepository.findAll()).isEmpty();
    }

    @Test
    void testDeleteTableBlocksCrossTenantAccess() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user2)
                .tableName("User2 Table")
                .schemaJson("{\"columns\": []}")
                .build();
        table = tableDefinitionRepository.save(table);

        mockMvc.perform(delete("/api/tables/" + table.getTableId())
                        .header("Authorization", token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void testCreateTableSuccess() throws Exception {
        String payload = """
                {
                  "tableName": "Success Table",
                  "schema": {
                    "columns": [
                      { "name": "Item Code", "type": "string" },
                      { "name": "Cost", "type": "decimal" },
                      { "name": "Quantity", "type": "decimal" }
                    ]
                  },
                  "initialRows": [
                    { "Item Code": "A102", "Cost": "299.50", "Quantity": "10" },
                    { "Item Code": "B504", "Cost": "19.99", "Quantity": "5" }
                  ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tables")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token1))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tableId").exists())
                .andExpect(jsonPath("$.tableName").value("Success Table"));

        assertThat(tableDefinitionRepository.findAll()).hasSize(1);
        assertThat(recordRepository.findAll()).hasSize(2);
    }

    @Test
    void testCreateTableValidationFailure() throws Exception {
        String payload = """
                {
                  "tableName": "Fail Table",
                  "schema": {
                    "columns": [
                      { "name": "Cost", "type": "decimal" }
                    ]
                  },
                  "initialRows": [
                    { "Cost": "not-a-decimal" }
                  ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tables")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILURE"))
                .andExpect(jsonPath("$.message").value("Value 'not-a-decimal' in column 'Cost' is not a valid decimal"));
    }

    @Test
    void testCreateTableRollbackOnError() throws Exception {
        String payload = """
                {
                  "tableName": "Rollback Table",
                  "schema": {
                    "columns": [
                      { "name": "Count", "type": "decimal" }
                    ]
                  },
                  "initialRows": [
                    { "Count": "12" },
                    { "Count": "invalid-int" }
                  ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tables")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILURE"));

        assertThat(tableDefinitionRepository.findAll()).isEmpty();
        assertThat(recordRepository.findAll()).isEmpty();
    }

    @Test
    void testGetTableDetailSuccess() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user1)
                .tableName("Detail Table")
                .schemaJson("{\"columns\": [{\"name\": \"Age\", \"type\": \"decimal\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        mockMvc.perform(get("/api/tables/" + table.getTableId())
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tableId").value(table.getTableId().toString()))
                .andExpect(jsonPath("$.tableName").value("Detail Table"))
                .andExpect(jsonPath("$.schema.columns[0].name").value("Age"))
                .andExpect(jsonPath("$.schema.columns[0].type").value("decimal"));
    }

    @Test
    void testGetTableDetailForbidden() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user2)
                .tableName("User2 Detail Table")
                .schemaJson("{\"columns\": []}")
                .build();
        table = tableDefinitionRepository.save(table);

        mockMvc.perform(get("/api/tables/" + table.getTableId())
                        .header("Authorization", token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void testGetTableRecordsSuccess() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user1)
                .tableName("Records Table")
                .schemaJson("{\"columns\": [{\"name\": \"Key\", \"type\": \"string\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        Record record1 = Record.builder()
                .user(user1)
                .tableDefinition(table)
                .data("{\"Key\": \"Value1\"}")
                .build();
        recordRepository.save(record1);

        Record record2 = Record.builder()
                .user(user1)
                .tableDefinition(table)
                .data("{\"Key\": \"Value2\"}")
                .build();
        recordRepository.save(record2);

        mockMvc.perform(get("/api/tables/" + table.getTableId() + "/records")
                        .param("page", "0")
                        .param("size", "10")
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].data.Key").exists());
    }

    @Test
    void testGetTableRecordsForbidden() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user2)
                .tableName("User2 Records Table")
                .schemaJson("{\"columns\": []}")
                .build();
        table = tableDefinitionRepository.save(table);

        mockMvc.perform(get("/api/tables/" + table.getTableId() + "/records")
                        .header("Authorization", token1))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
    }

    @Test
    void testAppendRecordsSuccess() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user1)
                .tableName("Append Table")
                .schemaJson("{\"columns\": [{\"name\": \"Code\", \"type\": \"string\"}, {\"name\": \"Price\", \"type\": \"decimal\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        String payload = """
                {
                  "records": [
                    { "Code": "Z99", "Price": "199.99" },
                    { "Code": "Y88", "Price": "9.95" }
                  ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tables/" + table.getTableId() + "/records")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token1))
                .andExpect(status().isOk());

        assertThat(recordRepository.findAll()).hasSize(2);
    }

    @Test
    void testAppendRecordsValidationFailure() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user1)
                .tableName("Append Fail Table")
                .schemaJson("{\"columns\": [{\"name\": \"Qty\", \"type\": \"decimal\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        String payload = """
                {
                  "records": [
                    { "Qty": "not-a-decimal" }
                  ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tables/" + table.getTableId() + "/records")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("VALIDATION_FAILURE"));
    }

    @Test
    void testAppendRecordsRollbackOnError() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user1)
                .tableName("Append Rollback Table")
                .schemaJson("{\"columns\": [{\"name\": \"Qty\", \"type\": \"decimal\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        String payload = """
                {
                  "records": [
                    { "Qty": "100" },
                    { "Qty": "invalid-int" }
                  ]
                }
                """;

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tables/" + table.getTableId() + "/records")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token1))
                .andExpect(status().isBadRequest());

        assertThat(recordRepository.findAll()).isEmpty();
    }

    @Test
    void testSearchByDateRange() throws Exception {
        TableDefinition table = TableDefinition.builder()
                .user(user1)
                .tableName("Date Search Table")
                .schemaJson("{\"columns\": [{\"name\": \"Item\", \"type\": \"string\"}]}")
                .build();
        table = tableDefinitionRepository.save(table);

        Record record = Record.builder()
                .user(user1)
                .tableDefinition(table)
                .data("{\"Item\": \"Widget\"}")
                .build();
        recordRepository.save(record);

        String todayStr = java.time.LocalDate.now().toString();
        String payload = """
                {
                  "filters": [
                    { "column": "DATE", "operator": "Between", "value": "2020-01-01", "maxValue": "%s" }
                  ],
                  "page": 0,
                  "size": 10
                }
                """.formatted(todayStr);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/tables/" + table.getTableId() + "/search")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(payload)
                        .header("Authorization", token1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].data.Item").value("Widget"))
                .andExpect(jsonPath("$.content[0].updatedAt").exists());
    }
}
