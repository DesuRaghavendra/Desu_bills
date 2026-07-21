package com.system.repository;

import com.system.entity.User;
import com.system.entity.TableDefinition;
import com.system.entity.Record;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RepositoryTests {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TableDefinitionRepository tableDefinitionRepository;

    @Autowired
    private RecordRepository recordRepository;

    @Test
    void testEntityMappingAndPersistence() {
        // 1. Save User
        User user = User.builder()
                .username("testuser")
                .email("test@example.com")
                .passwordHash("hashed")
                .build();
        user = userRepository.save(user);
        assertThat(user.getId()).isNotNull();

        // 2. Save Table Definition
        TableDefinition tableDef = TableDefinition.builder()
                .user(user)
                .tableName("Inventory")
                .schemaJson("{\"columns\": [{\"name\": \"item\", \"type\": \"string\"}]}")
                .build();
        tableDef = tableDefinitionRepository.save(tableDef);
        assertThat(tableDef.getTableId()).isNotNull();

        // 3. Save Record
        Record record = Record.builder()
                .user(user)
                .tableDefinition(tableDef)
                .data("{\"item\": \"laptop\"}")
                .build();
        record = recordRepository.save(record);
        assertThat(record.getRecordId()).isNotNull();

        // 4. Verification Queries
        Optional<TableDefinition> foundTable = tableDefinitionRepository.findByUser_IdAndTableName(user.getId(), "Inventory");
        assertThat(foundTable).isPresent();
        assertThat(foundTable.get().getSchemaJson()).contains("item");

        long recordCount = recordRepository.countByTableDefinition(tableDef);
        assertThat(recordCount).isEqualTo(1);
    }
}
