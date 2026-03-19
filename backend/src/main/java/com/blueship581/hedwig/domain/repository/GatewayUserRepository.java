package com.blueship581.hedwig.domain.repository;

import com.blueship581.hedwig.domain.entity.GatewayUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GatewayUserRepository extends JpaRepository<GatewayUser, Long> {

    Optional<GatewayUser> findByUsername(String username);

    boolean existsByUsername(String username);
}
