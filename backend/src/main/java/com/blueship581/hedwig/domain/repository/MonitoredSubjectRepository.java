package com.blueship581.hedwig.domain.repository;

import com.blueship581.hedwig.domain.entity.MonitoredSubject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MonitoredSubjectRepository extends JpaRepository<MonitoredSubject, Long> {

    List<MonitoredSubject> findByVendorConnectionId(Long vendorConnectionId);

    List<MonitoredSubject> findByVendorConnectionIdAndIsActive(Long vendorConnectionId, Boolean isActive);

    Optional<MonitoredSubject> findByVendorConnectionIdAndVendorSubjectId(Long vendorConnectionId, String vendorSubjectId);
}
