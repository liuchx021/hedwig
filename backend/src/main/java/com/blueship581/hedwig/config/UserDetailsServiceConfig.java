package com.blueship581.hedwig.config;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.blueship581.hedwig.domain.entity.GatewayUser;
import com.blueship581.hedwig.domain.mapper.GatewayUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class UserDetailsServiceConfig {

  private final GatewayUserMapper gatewayUserMapper;

  @Bean
  public UserDetailsService userDetailsService() {
    return username ->
        Optional.ofNullable(
                gatewayUserMapper.selectOne(
                    Wrappers.lambdaQuery(GatewayUser.class).eq(GatewayUser::getUsername, username)))
            .map(
                user ->
                    org.springframework.security.core.userdetails.User.builder()
                        .username(user.getUsername())
                        .password(user.getPasswordHash())
                        .roles("USER")
                        .build())
            .orElseThrow(() -> new UsernameNotFoundException("用户不存在：" + username));
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
