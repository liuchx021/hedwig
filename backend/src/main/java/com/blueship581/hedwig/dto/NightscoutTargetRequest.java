package com.blueship581.hedwig.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NightscoutTargetRequest {
    private String name;
    private String baseUrl;
    private String apiSecret;
    private Long monitoredSubjectId;
    private Boolean isDefault;
}