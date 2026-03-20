package com.blueship581.hedwig.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NightscoutTargetDto {
    private Long id;
    private String name;
    private String baseUrl;
    private String apiSecretHint;
    private String status;
    private Boolean isDefault;
    private Long monitoredSubjectId;
    private String monitoredSubjectName;
    private Instant lastPushAt;
    private Instant lastSuccessAt;
    private String lastErrorMessage;
    private Instant createdAt;
}