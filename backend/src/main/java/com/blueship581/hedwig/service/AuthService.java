package com.blueship581.hedwig.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.GatewayUser;
import com.blueship581.hedwig.domain.mapper.GatewayUserMapper;
import com.blueship581.hedwig.dto.AuthResponse;
import com.blueship581.hedwig.dto.LoginRequest;
import com.blueship581.hedwig.dto.RegisterRequest;
import com.blueship581.hedwig.exception.AuthException;
import com.blueship581.hedwig.exception.ErrorCode;
import com.blueship581.hedwig.security.JwtTokenProvider;
import org.springframework.security.authentication.BadCredentialsException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final GatewayUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        boolean exists = userMapper.exists(
                Wrappers.lambdaQuery(GatewayUser.class)
                        .eq(GatewayUser::getUsername, request.getUsername()));
        if (exists) {
            throw new AuthException(ErrorCode.USER_ALREADY_EXISTS, "用户名已存在：" + request.getUsername());
        }

        GatewayUser user = GatewayUser.builder()
                .username(request.getUsername())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .build();

        userMapper.insert(user);

        String token = jwtTokenProvider.generateToken(user.getUsername());
        return AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .message("注册成功")
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            throw new AuthException(ErrorCode.BAD_CREDENTIALS, "用户名或密码错误");
        }

        String token = jwtTokenProvider.generateToken(request.getUsername());
        return AuthResponse.builder()
                .token(token)
                .username(request.getUsername())
                .message("登录成功")
                .build();
    }
}
