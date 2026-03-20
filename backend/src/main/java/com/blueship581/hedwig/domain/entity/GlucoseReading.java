package com.blueship581.hedwig.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.blueship581.hedwig.domain.enums.TrendDirection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("glucose_readings")
public class GlucoseReading {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long monitoredSubjectId;

    private Double glucoseMmol;

    private Double glucoseMgdl;

    @Builder.Default
    private TrendDirection trendDirection = TrendDirection.NONE;

    private Instant readingTime;

    @Builder.Default
    private Boolean pushedToNightscout = false;
}
