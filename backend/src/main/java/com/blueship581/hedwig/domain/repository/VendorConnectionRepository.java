package com.blueship581.hedwig.domain.repository;

import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.enums.VendorType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VendorConnectionRepository extends JpaRepository<VendorConnection, Long> {

    List<VendorConnection> findByGatewayUserId(Long gatewayUserId);

    Optional<VendorConnection> findByGatewayUserIdAndVendorType(Long gatewayUserId, VendorType vendorType);

    List<VendorConnection> findByTokenStatusNot(TokenStatus tokenStatus);

    List<VendorConnection> findAllByTokenStatusIn(List<TokenStatus> statuses);
}
