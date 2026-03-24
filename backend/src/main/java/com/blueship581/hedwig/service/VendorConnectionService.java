package com.blueship581.hedwig.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.GatewayUser;
import com.blueship581.hedwig.domain.entity.MonitoredSubject;
import com.blueship581.hedwig.domain.entity.VendorConnection;
import com.blueship581.hedwig.domain.enums.TokenStatus;
import com.blueship581.hedwig.domain.mapper.GatewayUserMapper;
import com.blueship581.hedwig.domain.mapper.MonitoredSubjectMapper;
import com.blueship581.hedwig.domain.mapper.VendorConnectionMapper;
import com.blueship581.hedwig.dto.ConnectByLoginRequest;
import com.blueship581.hedwig.dto.ConnectByTokenRequest;
import com.blueship581.hedwig.dto.VendorConnectionDto;
import com.blueship581.hedwig.exception.ErrorCode;
import com.blueship581.hedwig.exception.ResourceNotFoundException;
import com.blueship581.hedwig.exception.VendorException;
import com.blueship581.hedwig.vendor.client.VendorClient;
import com.blueship581.hedwig.vendor.client.VendorClientFactory;
import com.blueship581.hedwig.vendor.model.VendorLoginRequest;
import com.blueship581.hedwig.vendor.model.VendorSubject;
import com.blueship581.hedwig.vendor.model.VendorTokenInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.blueship581.hedwig.exception.AuthException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorConnectionService {

  private final VendorConnectionMapper connectionMapper;
  private final MonitoredSubjectMapper subjectMapper;
  private final GatewayUserMapper userMapper;
  private final VendorClientFactory vendorClientFactory;

  @Transactional
  public VendorConnectionDto connectByToken(String username, ConnectByTokenRequest request) {
    log.info(
        "connectByToken: username={}, vendorType={}, accessToken length={}",
        username,
        request.getVendorType(),
        request.getAccessToken() != null ? request.getAccessToken().length() : 0);
    GatewayUser user = findUser(username);
    VendorClient client = vendorClientFactory.getClient(request.getVendorType());
    VendorTokenInfo tokenInfo = client.validateToken(request.getAccessToken());

    log.info(
        "connectByToken: token valid={}, userId={}, expiresAt={}",
        tokenInfo.isValid(),
        tokenInfo.getUserId(),
        tokenInfo.getExpiresAt());

    if (!tokenInfo.isValid()) {
      throw new VendorException(ErrorCode.VENDOR_TOKEN_INVALID, "访问令牌无效或已过期，请重新获取后再连接");
    }

    VendorConnection connection =
        connectionMapper
            .selectList(
                Wrappers.lambdaQuery(VendorConnection.class)
                    .eq(VendorConnection::getGatewayUserId, user.getId())
                    .eq(VendorConnection::getVendorType, request.getVendorType()))
            .stream()
            .findFirst()
            .orElse(
                VendorConnection.builder()
                    .gatewayUserId(user.getId())
                    .vendorType(request.getVendorType())
                    .build());

    connection.setAccessToken(tokenInfo.getToken());
    connection.setVendorUserId(tokenInfo.getUserId());
    connection.setTokenExpiresAt(tokenInfo.getExpiresAt());
    connection.setTokenStatus(TokenStatus.ACTIVE);
    if (connection.getId() == null) {
      connectionMapper.insert(connection);
    } else {
      connectionMapper.updateById(connection);
    }

    // Sync monitored subjects
    syncSubjects(connection, client);

    return toDto(connection);
  }

  @Transactional
  public VendorConnectionDto connectByLogin(String username, ConnectByLoginRequest request) {
    GatewayUser user = findUser(username);
    VendorClient client = vendorClientFactory.getClient(request.getVendorType());

    VendorTokenInfo tokenInfo =
        client.login(
            VendorLoginRequest.builder()
                .username(request.getUsername())
                .password(request.getPassword())
                .build());

    if (!tokenInfo.isValid()) {
      throw new VendorException(ErrorCode.VENDOR_LOGIN_FAILED, "厂商账号登录失败，或返回的访问令牌无效");
    }

    VendorConnection connection =
        connectionMapper
            .selectList(
                Wrappers.lambdaQuery(VendorConnection.class)
                    .eq(VendorConnection::getGatewayUserId, user.getId())
                    .eq(VendorConnection::getVendorType, request.getVendorType()))
            .stream()
            .findFirst()
            .orElse(
                VendorConnection.builder()
                    .gatewayUserId(user.getId())
                    .vendorType(request.getVendorType())
                    .build());

    connection.setAccessToken(tokenInfo.getToken());
    connection.setVendorUserId(tokenInfo.getUserId());
    connection.setTokenExpiresAt(tokenInfo.getExpiresAt());
    connection.setTokenStatus(TokenStatus.ACTIVE);
    if (connection.getId() == null) {
      connectionMapper.insert(connection);
    } else {
      connectionMapper.updateById(connection);
    }

    syncSubjects(connection, client);

    return toDto(connection);
  }

  @Transactional
  public VendorConnectionDto refreshToken(String username, Long connectionId) {
    GatewayUser user = findUser(username);
    VendorConnection connection =
        Optional.ofNullable(connectionMapper.selectById(connectionId))
            .filter(c -> c.getGatewayUserId().equals(user.getId()))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ErrorCode.VENDOR_CONNECTION_NOT_FOUND, "设备连接不存在：" + connectionId));

    VendorClient client = vendorClientFactory.getClient(connection.getVendorType());
    VendorTokenInfo tokenInfo = client.validateToken(connection.getAccessToken());
    connection.setTokenExpiresAt(tokenInfo.getExpiresAt());
    connection.setTokenStatus(tokenInfo.isValid() ? TokenStatus.ACTIVE : TokenStatus.EXPIRED);
    connectionMapper.updateById(connection);

    return toDto(connection);
  }

  @Transactional
  public void disconnect(String username, Long connectionId) {
    GatewayUser user = findUser(username);
    VendorConnection connection =
        Optional.ofNullable(connectionMapper.selectById(connectionId))
            .filter(c -> c.getGatewayUserId().equals(user.getId()))
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        ErrorCode.VENDOR_CONNECTION_NOT_FOUND, "设备连接不存在：" + connectionId));

    // Deactivate all subjects for this connection
    subjectMapper
        .selectList(
            Wrappers.lambdaQuery(MonitoredSubject.class)
                .eq(MonitoredSubject::getVendorConnectionId, connection.getId()))
        .forEach(
            s -> {
              s.setIsActive(false);
              subjectMapper.updateById(s);
            });

    connectionMapper.deleteById(connection.getId());
  }

  public List<VendorConnectionDto> getConnections(String username) {
    GatewayUser user = findUser(username);
    return connectionMapper
        .selectList(
            Wrappers.lambdaQuery(VendorConnection.class)
                .eq(VendorConnection::getGatewayUserId, user.getId()))
        .stream()
        .map(this::toDto)
        .collect(Collectors.toList());
  }

  @Transactional
  public void syncSubjects(VendorConnection connection) {
    VendorClient client = vendorClientFactory.getClient(connection.getVendorType());
    syncSubjects(connection, client);
  }

  private void syncSubjects(VendorConnection connection, VendorClient client) {
    try {
      List<VendorSubject> vendorSubjects =
          client.getMonitoredSubjects(connection.getAccessToken(), connection.getVendorUserId());

      Instant now = Instant.now();
      for (VendorSubject vs : vendorSubjects) {
        Instant sensorExpiresAt =
            vs.getSensorRestSeconds() != null ? now.plusSeconds(vs.getSensorRestSeconds()) : null;

        Optional<MonitoredSubject> existingOpt =
            subjectMapper
                .selectList(
                    Wrappers.lambdaQuery(MonitoredSubject.class)
                        .eq(MonitoredSubject::getVendorConnectionId, connection.getId())
                        .eq(MonitoredSubject::getVendorSubjectId, vs.getSubjectId()))
                .stream()
                .findFirst();

        if (existingOpt.isPresent()) {
          MonitoredSubject existing = existingOpt.get();
          existing.setDisplayName(vs.getDisplayName());
          existing.setVendorDeviceId(vs.getDeviceId());
          existing.setIsActive(true);
          existing.setSensorExpiresAt(sensorExpiresAt);
          subjectMapper.updateById(existing);
        } else {
          subjectMapper.insert(
              MonitoredSubject.builder()
                  .vendorConnectionId(connection.getId())
                  .vendorSubjectId(vs.getSubjectId())
                  .vendorDeviceId(vs.getDeviceId())
                  .displayName(vs.getDisplayName())
                  .isActive(true)
                  .sensorExpiresAt(sensorExpiresAt)
                  .build());
        }
      }
    } catch (Exception e) {
      log.warn("Failed to sync subjects for connection {}: {}", connection.getId(), e.getMessage());
    }
  }

  private GatewayUser findUser(String username) {
    GatewayUser user =
        userMapper.selectOne(
            Wrappers.lambdaQuery(GatewayUser.class).eq(GatewayUser::getUsername, username));
    if (user == null) {
      throw new AuthException(ErrorCode.USER_NOT_FOUND, "用户不存在：" + username);
    }
    return user;
  }

  private VendorConnectionDto toDto(VendorConnection c) {
    MonitoredSubject primarySubject =
        subjectMapper
            .selectList(
                Wrappers.lambdaQuery(MonitoredSubject.class)
                    .eq(MonitoredSubject::getVendorConnectionId, c.getId())
                    .eq(MonitoredSubject::getIsActive, true))
            .stream()
            .min(Comparator.comparing(MonitoredSubject::getId))
            .orElse(null);

    return VendorConnectionDto.builder()
        .id(c.getId())
        .vendorType(c.getVendorType())
        .vendorUserId(c.getVendorUserId())
        .primarySubjectId(primarySubject != null ? primarySubject.getId() : null)
        .primarySubjectName(primarySubject != null ? primarySubject.getDisplayName() : null)
        .tokenStatus(c.getTokenStatus())
        .tokenExpiresAt(c.getTokenExpiresAt())
        .lastSyncedAt(c.getLastSyncedAt())
        .sensorExpiresAt(primarySubject != null ? primarySubject.getSensorExpiresAt() : null)
        .build();
  }
}
