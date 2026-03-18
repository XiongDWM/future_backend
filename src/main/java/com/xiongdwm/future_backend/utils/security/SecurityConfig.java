package com.xiongdwm.future_backend.utils.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserActivityTracker activityTracker;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, UserActivityTracker activityTracker) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.activityTracker = activityTracker;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                // 禁用不需要的功能
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                // 无状态，不创建 session
                .securityContextRepository(
                        org.springframework.security.web.server.context.NoOpServerSecurityContextRepository.getInstance()
                )

                // 路径放行规则
                .authorizeExchange(exchanges -> exchanges
                        // 握手 + 登录：完全放行
                        .pathMatchers("/crypto/handshake").permitAll()
                        .pathMatchers("/user/login").permitAll()
                        .pathMatchers("/user/pal/login").permitAll()
                        // SSE 事件流放行（已在 GlobalWebFilter 中校验 session + 签名）
                        .pathMatchers("/events/stream").permitAll()
                        // 图片预览放行
                        .pathMatchers("/oss/preview/**").permitAll()
                        // OPTIONS 预检放行
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        // 其他所有请求需要认证
                        .anyExchange().authenticated()
                )

                // 未认证时返回 401，不跳转登录页
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((exchange, denied) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return exchange.getResponse().setComplete();
                        })
                        .accessDeniedHandler((exchange, denied) -> {
                            exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                            return exchange.getResponse().setComplete();
                        })
                )

                // 在认证过滤器位置插入 JWT 过滤器
                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }

    /**
     * JWT 认证过滤器：从 Authorization header 提取 token，
     * 验证通过后设置 SecurityContext，仅此而已。
     * 不碰 body、不碰 X-Session-Id、不碰任何加解密逻辑。
     */
    private WebFilter jwtAuthenticationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            // 放行路径不做 token 校验（与上面 permitAll 一致，双重保障）
            if ("/crypto/handshake".equals(path)
                    || "/user/login".equals(path)
                    || "/user/pal/login".equals(path)
                    || "/events/stream".equals(path)
                    || path.startsWith("/oss/preview/")) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange); // 没有 token → 交给 Security 框架返回 401
            }

            String token = authHeader.substring(7);
            var claims = jwtTokenProvider.parseToken(token);
            if (claims == null) {
                return chain.filter(exchange); // token 无效 → 交给 Security 框架返回 401
            }

            // token 有效 → 设置认证信息
            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);

            // 滑动续期：token 过半生命周期，签发新 token 通过响应头返回
            if (jwtTokenProvider.shouldRenew(claims)) {
                String newToken = jwtTokenProvider.renewToken(claims);
                exchange.getResponse().getHeaders().set("X-New-Token", newToken);
            }

            try { activityTracker.touch(Long.parseLong(userId)); } catch (NumberFormatException ignored) {}

            return chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
        };
    }
}
