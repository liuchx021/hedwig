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
@TableName("monitored_subjects")
public class MonitoredSubject {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long vendorConnectionId;

    private String vendorSubjectId;

    private String vendorDeviceId;

    private String displayName;

    @Builder.Default
    private Boolean isActive = true;

    private Instant sensorExpiresAt;
}
