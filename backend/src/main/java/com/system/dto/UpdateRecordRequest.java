package com.system.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateRecordRequest {

    @NotNull(message = "Data payload cannot be null")
    private Map<String, Object> data;
}
