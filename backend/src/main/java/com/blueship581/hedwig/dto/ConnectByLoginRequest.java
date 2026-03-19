package com.blueship581.hedwig.dto;

import com.blueship581.hedwig.domain.enums.VendorType;
import lombok.Data;

@Data
public class ConnectByLoginRequest {
    private VendorType vendorType;
    private String username;
    private String password;
}
