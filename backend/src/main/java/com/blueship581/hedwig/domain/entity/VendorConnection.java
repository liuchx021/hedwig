package com.blueship581.hedwig.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.enums.VendorType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("vendor_connections")
public class VendorConnection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long gatewayUserId;

    private VendorType vendorType;

    private String accessToken;

    private String vendorUserId;

    private Instant tokenExpiresAt;

    @Builder.Default
    private TokenStatus tokenStatus = TokenStatus.ACTIVE;

    private Instant lastSyncedAt;
}
