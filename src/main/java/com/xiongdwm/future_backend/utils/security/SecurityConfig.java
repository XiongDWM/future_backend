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

import com.xiongdwm.future_backend.utils.tenant.TenantContextPropagation;
import com.xiongdwm.future_backend.utils.tenant.TenantRoutingDataSource;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserActivityTracker activityTracker;
    private final TenantRoutingDataSource tenantRoutingDataSource;

    public SecurityConfig(JwtTokenProvider jwtTokenProvider, UserActivityTracker activityTracker,
                          TenantRoutingDataSource tenantRoutingDataSource) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.activityTracker = activityTracker;
        this.tenantRoutingDataSource = tenantRoutingDataSource;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())

                .securityContextRepository(
                        org.springframework.security.web.server.context.NoOpServerSecurityContextRepository.getInstance()
                )

                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/crypto/handshake").permitAll()
                        .pathMatchers("/user/login").permitAll()
                        .pathMatchers("/user/pal/login").permitAll()
                        .pathMatchers("/studio/register").permitAll()
                        .pathMatchers("/events/stream").permitAll()
                        .pathMatchers("/oss/preview/**").permitAll()
                        .pathMatchers(HttpMethod.OPTIONS).permitAll()
                        .anyExchange().authenticated()
                )

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

                .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)

                .build();
    }

    private WebFilter jwtAuthenticationFilter() {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getPath().value();

            if ("/crypto/handshake".equals(path)
                    || "/user/login".equals(path)
                    || "/user/pal/login".equals(path)
                    || "/studio/register".equals(path)
                    || "/events/stream".equals(path)
                    || path.startsWith("/oss/preview/")) {
                return chain.filter(exchange);
            }

            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange);
            }

            String token = authHeader.substring(7);
            var claims = jwtTokenProvider.parseToken(token);
            if (claims == null) {
                return chain.filter(exchange);
            }

            String userId = claims.getSubject();
            String role = claims.get("role", String.class);
            var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
            var auth = new UsernamePasswordAuthenticationToken(userId, null, authorities);

            if (jwtTokenProvider.shouldRenew(claims)) {
                String newToken = jwtTokenProvider.renewToken(claims);
                exchange.getResponse().getHeaders().set("X-New-Token", newToken);
            }

            try { activityTracker.touch(Long.parseLong(userId)); } catch (NumberFormatException ignored) {}

            // 从 JWT 中提取 studioId，通过 Reactor Context 传播到 ThreadLocal（自动路由租户库）
            Long studioId = jwtTokenProvider.getStudioId(claims);
            String dbName = studioId != null ? tenantRoutingDataSource.getDbName(studioId) : null;

            var result = chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
            if (dbName != null) {
                final Long sid = studioId;
                result = result.contextWrite(ctx -> ctx.put(TenantContextPropagation.KEY, dbName)
                                                       .put(TenantContextPropagation.STUDIO_ID_KEY, sid));
            }
            return result;
        };
    }
}
