package com.blueship581.hedwig.domain.enums;

import com.alibaba.fastjson2.JSON;
import com.blueship581.hedwig.dto.ConnectByTokenRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VendorTypeTest {

    @Test
    void acceptsLegacySiSensingAlias() {
        ConnectByTokenRequest request = JSON.parseObject("""
                {"vendorType":"SI_SENSING","accessToken":"demo-token"}
                """, ConnectByTokenRequest.class);

        assertEquals(VendorType.SISENSING, request.getVendorType());
    }
}
