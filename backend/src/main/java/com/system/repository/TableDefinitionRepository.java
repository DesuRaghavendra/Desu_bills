package com.system.repository;

import com.system.entity.TableDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TableDefinitionRepository extends JpaRepository<TableDefinition, UUID> {
    List<TableDefinition> findByUser_Id(UUID userId);
    Optional<TableDefinition> findByUser_IdAndTableName(UUID userId, String tableName);
}
