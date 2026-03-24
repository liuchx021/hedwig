package com.blueship581.hedwig.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.GatewayUser;
import com.blueship581.hedwig.domain.entity.MonitoredSubject;
import com.blueship581.hedwig.domain.entity.NightscoutTarget;
import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.mapper.GatewayUserMapper;
import com.blueship581.hedwig.domain.mapper.MonitoredSubjectMapper;
import com.blueship581.hedwig.domain.mapper.NightscoutTargetMapper;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
import com.blueship581.hedwig.dto.NightscoutTargetDto;
import com.blueship581.hedwig.dto.NightscoutTargetRequest;
import com.blueship581.hedwig.exception.AuthException;
import com.blueship581.hedwig.exception.BusinessException;
import com.blueship581.hedwig.exception.ErrorCode;
import com.blueship581.hedwig.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NightscoutTargetService {

    private final NightscoutTargetMapper targetMapper;
    private final GatewayUserMapper userMapper;
    private final MonitoredSubjectMapper subjectMapper;
    private final VendorConnectionMapper connectionMapper;

    public List<NightscoutTargetDto> listTargets(String username) {
        GatewayUser user = findUser(username);
        List<NightscoutTarget> targets = targetMapper.selectList(
                Wrappers.lambdaQuery(NightscoutTarget.class)
                        .eq(NightscoutTarget::getGatewayUserId, user.getId())
                        .orderByDesc(NightscoutTarget::getIsDefault)
                        .orderByAsc(NightscoutTarget::getId));
        return targets.stream().map(this::toDto).collect(Collectors.toList());
    }

    public NightscoutTargetDto getTarget(String username, Long targetId) {
        NightscoutTarget target = findTarget(username, targetId);
        return toDto(target);
    }

    @Transactional
    public NightscoutTargetDto createTarget(String username, NightscoutTargetRequest request) {
        GatewayUser user = findUser(username);

        // 校验名称唯一性
        boolean nameExists = targetMapper.exists(
                Wrappers.lambdaQuery(NightscoutTarget.class)
                        .eq(NightscoutTarget::getGatewayUserId, user.getId())
                        .eq(NightscoutTarget::getName, request.getName()));
        if (nameExists) {
            throw new BusinessException(ErrorCode.NIGHTSCOUT_NAME_DUPLICATE,
                    "Nightscout 目标名称已存在：" + request.getName());
        }

        // 校验 monitoredSubjectId 归属
        if (request.getMonitoredSubjectId() != null) {
            validateSubjectBelongsToUser(user.getId(), request.getMonitoredSubjectId());
        }

        String apiSecretSha1 = sha1(request.getApiSecret());
        String hint = request.getApiSecret().length() >= 4
                ? "****" + request.getApiSecret().substring(request.getApiSecret().length() - 4)
                : "****";

        // 如果设为默认，先取消当前默认
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(user.getId());
        }

        NightscoutTarget target = NightscoutTarget.builder()
                .gatewayUserId(user.getId())
                .monitoredSubjectId(request.getMonitoredSubjectId())
                .name(request.getName())
                .baseUrl(normalizeUrl(request.getBaseUrl()))
                .apiSecretSha1(apiSecretSha1)
                .apiSecretHint(hint)
                .status("ACTIVE")
                .isDefault(Boolean.TRUE.equals(request.getIsDefault()))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        targetMapper.insert(target);
        log.info("Created Nightscout target: id={}, name={}, user={}", target.getId(), target.getName(), username);
        return toDto(target);
    }

    @Transactional
    public NightscoutTargetDto updateTarget(String username, Long targetId, NightscoutTargetRequest request) {
        NightscoutTarget target = findTarget(username, targetId);

        // 名称变更时检查唯一性
        if (request.getName() != null && !request.getName().equals(target.getName())) {
            boolean nameExists = targetMapper.exists(
                    Wrappers.lambdaQuery(NightscoutTarget.class)
                            .eq(NightscoutTarget::getGatewayUserId, target.getGatewayUserId())
                            .eq(NightscoutTarget::getName, request.getName())
                            .ne(NightscoutTarget::getId, targetId));
            if (nameExists) {
                throw new BusinessException(ErrorCode.NIGHTSCOUT_NAME_DUPLICATE,
                        "Nightscout 目标名称已存在：" + request.getName());
            }
            target.setName(request.getName());
        }

        if (request.getBaseUrl() != null) {
            target.setBaseUrl(normalizeUrl(request.getBaseUrl()));
        }

        if (request.getApiSecret() != null && !request.getApiSecret().isBlank()) {
            target.setApiSecretSha1(sha1(request.getApiSecret()));
            String hint = request.getApiSecret().length() >= 4
                    ? "****" + request.getApiSecret().substring(request.getApiSecret().length() - 4)
                    : "****";
            target.setApiSecretHint(hint);
        }

        if (request.getMonitoredSubjectId() != null) {
            validateSubjectBelongsToUser(target.getGatewayUserId(), request.getMonitoredSubjectId());
            target.setMonitoredSubjectId(request.getMonitoredSubjectId());
        }

        if (Boolean.TRUE.equals(request.getIsDefault())) {
            clearDefault(target.getGatewayUserId());
            target.setIsDefault(true);
        } else if (request.getIsDefault() != null) {
            target.setIsDefault(false);
        }

        target.setUpdatedAt(Instant.now());
        targetMapper.updateById(target);
        log.info("Updated Nightscout target: id={}, name={}", targetId, target.getName());
        return toDto(target);
    }

    @Transactional
    public void deleteTarget(String username, Long targetId) {
        NightscoutTarget target = findTarget(username, targetId);
        targetMapper.deleteById(target.getId());
        log.info("Deleted Nightscout target: id={}, name={}", targetId, target.getName());
    }

    @Transactional
    public NightscoutTargetDto toggleStatus(String username, Long targetId) {
        NightscoutTarget target = findTarget(username, targetId);
        String newStatus = "ACTIVE".equals(target.getStatus()) ? "DISABLED" : "ACTIVE";
        target.setStatus(newStatus);
        target.setUpdatedAt(Instant.now());
        targetMapper.updateById(target);
        log.info("Toggled Nightscout target status: id={}, status={}", targetId, newStatus);
        return toDto(target);
    }

    /**
     * 获取当前用户所有 ACTIVE 的推送目标（供同步调度使用）。
     */
    public List<NightscoutTarget> getActiveTargets(Long gatewayUserId) {
        return targetMapper.selectList(
                Wrappers.lambdaQuery(NightscoutTarget.class)
                        .eq(NightscoutTarget::getGatewayUserId, gatewayUserId)
                        .eq(NightscoutTarget::getStatus, "ACTIVE"));
    }

    /**
     * 获取所有 ACTIVE 的推送目标（供全局调度使用）。
     */
    public List<NightscoutTarget> getAllActiveTargets() {
        return targetMapper.selectList(
                Wrappers.lambdaQuery(NightscoutTarget.class)
                        .eq(NightscoutTarget::getStatus, "ACTIVE"));
    }

    /**
     * 更新目标的推送状态信息。
     */
    public void updatePushStatus(Long targetId, boolean success, String errorMessage) {
        NightscoutTarget target = targetMapper.selectById(targetId);
        if (target == null) return;

        target.setLastPushAt(Instant.now());
        if (success) {
            target.setLastSuccessAt(Instant.now());
            target.setLastErrorMessage(null);
        } else {
            target.setLastErrorMessage(errorMessage);
        }
        target.setUpdatedAt(Instant.now());
        targetMapper.updateById(target);
    }

    // ==================== 内部方法 ====================

    private NightscoutTarget findTarget(String username, Long targetId) {
        GatewayUser user = findUser(username);
        return Optional.ofNullable(targetMapper.selectById(targetId))
                .filter(t -> t.getGatewayUserId().equals(user.getId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.NIGHTSCOUT_TARGET_NOT_FOUND,
                        "Nightscout 目标不存在：" + targetId));
    }

    private GatewayUser findUser(String username) {
        GatewayUser user = userMapper.selectOne(
                Wrappers.lambdaQuery(GatewayUser.class)
                        .eq(GatewayUser::getUsername, username));
        if (user == null) {
            throw new AuthException(ErrorCode.USER_NOT_FOUND, "用户不存在：" + username);
        }
        return user;
    }

    private void validateSubjectBelongsToUser(Long gatewayUserId, Long subjectId) {
        MonitoredSubject subject = subjectMapper.selectById(subjectId);
        if (subject == null) {
            throw new ResourceNotFoundException(ErrorCode.SUBJECT_NOT_FOUND,
                    "监测对象不存在：" + subjectId);
        }
        // Verify ownership: subject → vendorConnection → gatewayUserId
        if (subject.getVendorConnectionId() == null) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "该监测对象未关联任何设备连接，无法绑定");
        }
        VendorConnection connection = connectionMapper.selectById(subject.getVendorConnectionId());
        if (connection == null || !connection.getGatewayUserId().equals(gatewayUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED,
                    "没有权限操作该监测对象：" + subjectId);
        }
    }

    private void clearDefault(Long gatewayUserId) {
        List<NightscoutTarget> defaults = targetMapper.selectList(
                Wrappers.lambdaQuery(NightscoutTarget.class)
                        .eq(NightscoutTarget::getGatewayUserId, gatewayUserId)
                        .eq(NightscoutTarget::getIsDefault, true));
        for (NightscoutTarget t : defaults) {
            t.setIsDefault(false);
            t.setUpdatedAt(Instant.now());
            targetMapper.updateById(t);
        }
    }

    private NightscoutTargetDto toDto(NightscoutTarget target) {
        String subjectName = null;
        if (target.getMonitoredSubjectId() != null) {
            MonitoredSubject subject = subjectMapper.selectById(target.getMonitoredSubjectId());
            if (subject != null) {
                subjectName = subject.getDisplayName();
            }
        }

        return NightscoutTargetDto.builder()
                .id(target.getId())
                .name(target.getName())
                .baseUrl(target.getBaseUrl())
                .apiSecretHint(target.getApiSecretHint())
                .status(target.getStatus())
                .isDefault(target.getIsDefault())
                .monitoredSubjectId(target.getMonitoredSubjectId())
                .monitoredSubjectName(subjectName)
                .lastPushAt(target.getLastPushAt())
                .lastSuccessAt(target.getLastSuccessAt())
                .lastErrorMessage(target.getLastErrorMessage())
                .createdAt(target.getCreatedAt())
                .build();
    }

    private String normalizeUrl(String url) {
        if (url != null && url.endsWith("/")) {
            return url.substring(0, url.length() - 1);
        }
        return url;
    }

    private String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] hash = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("系统不支持 SHA-1 摘要算法", e);
        }
    }
}