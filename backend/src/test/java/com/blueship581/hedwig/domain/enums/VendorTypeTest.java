package com.blueship581.hedwig.domain.enums;

import com.blueship581.hedwig.dto.ConnectByTokenRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VendorTypeTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void acceptsLegacySiSensingAlias() throws Exception {
        ConnectByTokenRequest request = objectMapper.readValue("""
                {"vendorType":"SI_SENSING","accessToken":"demo-token"}
                """, ConnectByTokenRequest.class);

        assertEquals(VendorType.SISENSING, request.getVendorType());
    }
}
