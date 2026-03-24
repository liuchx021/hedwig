package com.blueship581.hedwig.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("nightscout_targets")
public class NightscoutTarget {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long gatewayUserId;

  private Long monitoredSubjectId;

  private String name;

  private String baseUrl;

  private String apiSecretSha1;

  private String apiSecretHint;

  @Builder.Default private String status = "ACTIVE";

  @Builder.Default private Boolean isDefault = false;

  private Instant lastPushAt;

  private Instant lastSuccessAt;

  private String lastErrorMessage;

  private Instant createdAt;

  private Instant updatedAt;
}
