package com.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BatchDeleteRequest {

    @NotEmpty(message = "Record IDs list cannot be empty")
    private List<UUID> recordIds;
}
