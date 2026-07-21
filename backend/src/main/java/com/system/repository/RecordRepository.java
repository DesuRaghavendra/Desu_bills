package com.system.repository;

import com.system.entity.Record;
import com.system.entity.TableDefinition;
import com.system.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface RecordRepository extends JpaRepository<Record, UUID> {
    Page<Record> findByTableDefinitionAndUser(TableDefinition tableDefinition, User user, Pageable pageable);
    long countByTableDefinition(TableDefinition tableDefinition);
    void deleteByTableDefinition(TableDefinition tableDefinition);
    List<Record> findAllByRecordIdIn(List<UUID> recordIds);
    void deleteAllByRecordIdIn(List<UUID> recordIds);
}

