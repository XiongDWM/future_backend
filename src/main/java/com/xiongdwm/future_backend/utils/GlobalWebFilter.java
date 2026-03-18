package com.xiongdwm.future_backend.utils;

import java.nio.charset.StandardCharsets;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;

import org.reactivestreams.Publisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.utils.cache.CacheHandler;
import com.xiongdwm.future_backend.utils.cache.LRUCache;
import com.xiongdwm.future_backend.utils.ecc.CryptoSessionConfig;
import com.xiongdwm.future_backend.utils.ecc.ECDHKeyGen;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
public class GlobalWebFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(GlobalWebFilter.class);

    private static final String SESSION_HEADER = "X-Session-Id";
    private static final String TIMESTAMP_HEADER = "X-Timestamp";
    private static final String SIGNATURE_HEADER = "X-Signature";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 签名时间戳容差：5 分钟 */
    private static final long TIMESTAMP_TOLERANCE_MS = 5 * 60 * 1000L;

    /** 完全放行：仅握手接口不做任何校验和加解密 */
    private static final Set<String> FULLY_EXCLUDED_PATHS = Set.of(
            "/crypto/handshake"
    );

    /** 前缀匹配放行：如图片预览等无需加解密的路径 */
    private static final Set<String> FULLY_EXCLUDED_PREFIXES = Set.of(
            "/oss/preview/"
    );

    /** 仅校验 session + 签名，不做 body 加解密（SSE 等流式接口、文件上传） */
    private static final Set<String> SESSION_ONLY_PATHS = Set.of(
            "/events/stream",
            "/oss/upload"
    );

    private final CacheHandler cacheHandler;
    private final boolean devMode;

    public GlobalWebFilter(CacheHandler cacheHandler,
                           @Value("${project.dev:false}") boolean devMode) {
        this.cacheHandler = cacheHandler;
        this.devMode = devMode;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (devMode) {
            return chain.filter(exchange);
        }
        String path = exchange.getRequest().getPath().value();

        // 握手接口完全放行
        if (FULLY_EXCLUDED_PATHS.contains(path)) {
            logger.info("握手接口放行");
            return chain.filter(exchange);
        }

        // 前缀匹配放行（图片预览等）
        for (String prefix : FULLY_EXCLUDED_PREFIXES) {
            if (path.startsWith(prefix)) {
                return chain.filter(exchange);
            }
        }

        // 没有 X-Session-Id → 要求先握手
        String sessionId = exchange.getRequest().getHeaders().getFirst(SESSION_HEADER);
        if (sessionId == null || sessionId.isBlank()) {
            return writeResponse(exchange, HttpStatus.FORBIDDEN, ApiResponse.unauthorized());
        }

        // 取共享密钥
        LRUCache<String, SecretKey> km = cacheHandler.getCache(CryptoSessionConfig.CACHE_KM);
        SecretKey sharedKey = km.peek(sessionId);
        if (sharedKey == null) {
            return writeResponse(exchange, HttpStatus.FORBIDDEN,
                    ApiResponse.key_expired());
        }

        // 验证时间戳
        String timestampStr = exchange.getRequest().getHeaders().getFirst(TIMESTAMP_HEADER);
        String signature = exchange.getRequest().getHeaders().getFirst(SIGNATURE_HEADER);
        if (timestampStr == null || signature == null) {
            return writeResponse(exchange, HttpStatus.FORBIDDEN,
                    ApiResponse.bussiness_error("缺少签名信息"));
        }

        try {
            long timestamp = Long.parseLong(timestampStr);
            long now = System.currentTimeMillis();
            if (Math.abs(now - timestamp) > TIMESTAMP_TOLERANCE_MS) {
                return writeResponse(exchange, HttpStatus.FORBIDDEN,
                        ApiResponse.bussiness_error("请求已过期"));
            }
        } catch (NumberFormatException e) {
            return writeResponse(exchange, HttpStatus.FORBIDDEN,
                    ApiResponse.bussiness_error("时间戳格式错误"));
        }

        String method = exchange.getRequest().getMethod().name();
        // 签名验证使用包含 query string 的完整路径，与前端 signRequest 一致
        String fullPath = exchange.getRequest().getURI().getRawPath();
        String query = exchange.getRequest().getURI().getRawQuery();
        if (query != null && !query.isEmpty()) {
            fullPath = fullPath + "?" + query;
        }

        // SSE 等流式接口：校验 session + 签名，不做 body 加解密
        if (SESSION_ONLY_PATHS.contains(path)) {
            // 无 body 签名: METHOD\npath\ntimestamp\n
            String message = method.toUpperCase() + "\n" + fullPath + "\n" + timestampStr + "\n";
            if (!ECDHKeyGen.verifySignature(sharedKey, message, signature)) {
                return writeResponse(exchange, HttpStatus.FORBIDDEN,
                        ApiResponse.bussiness_error("签名验证失败"));
            }
            return chain.filter(exchange);
        }

        // GET / DELETE / HEAD / OPTIONS：无 body，直接验签后放行
        if (isBodylessMethod(method)) {
            String message = method.toUpperCase() + "\n" + fullPath + "\n" + timestampStr + "\n";
            if (!ECDHKeyGen.verifySignature(sharedKey, message, signature)) {
                return writeResponse(exchange, HttpStatus.FORBIDDEN,
                        ApiResponse.bussiness_error("签名验证失败"));
            }
            ServerWebExchange encryptedExchange = wrapEncryptedResponse(exchange, sharedKey);
            return chain.filter(encryptedExchange);
        }

        // POST / PUT 等有 body 的请求：读 body → 验签 → 解密 → 放行
        return verifyAndDecryptRequestBody(exchange, sharedKey, method, fullPath, timestampStr, signature)
                .flatMap(decryptedExchange -> {
                    ServerWebExchange encryptedExchange = wrapEncryptedResponse(decryptedExchange, sharedKey);
                    return chain.filter(encryptedExchange);
                });
    }

    // ======================== 签名验证 + 解密请求体 ========================

    private static boolean isBodylessMethod(String method) {
        return "GET".equalsIgnoreCase(method) || "DELETE".equalsIgnoreCase(method)
                || "HEAD".equalsIgnoreCase(method) || "OPTIONS".equalsIgnoreCase(method);
    }

    /**
     * 有 body 的请求：读取原始 body → 验证签名 → 解密 → 替换 body
     */
    private Mono<ServerWebExchange> verifyAndDecryptRequestBody(
            ServerWebExchange exchange, SecretKey key,
            String method, String path, String timestampStr, String signature) {

        ServerHttpRequest request = exchange.getRequest();

        return DataBufferUtils.join(request.getBody())
                .defaultIfEmpty(DefaultDataBufferFactory.sharedInstance.wrap(new byte[0]))
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    if (bytes.length == 0) {
                        // 空 body，按无 body 验签
                        String message = method.toUpperCase() + "\n" + path + "\n" + timestampStr + "\n";
                        if (!ECDHKeyGen.verifySignature(key, message, signature)) {
                            return writeResponse(exchange, HttpStatus.FORBIDDEN,
                                    ApiResponse.bussiness_error("签名验证失败"))
                                    .then(Mono.<ServerWebExchange>empty());
                        }
                        return Mono.just(exchange);
                    }

                    try {
                        String payload = new String(bytes, StandardCharsets.UTF_8).trim();
                        // 去掉可能的 JSON 引号包裹: "xxx" → xxx
                        if (payload.startsWith("\"") && payload.endsWith("\"")) {
                            payload = payload.substring(1, payload.length() - 1);
                        }

                        // 签名消息: METHOD\npath\ntimestamp\nbody（body 是加密后的密文）
                        String message = method.toUpperCase() + "\n" + path + "\n" + timestampStr + "\n" + payload;
                        if (!ECDHKeyGen.verifySignature(key, message, signature)) {
                            return writeResponse(exchange, HttpStatus.FORBIDDEN,
                                    ApiResponse.bussiness_error("签名验证失败"))
                                    .then(Mono.<ServerWebExchange>empty());
                        }

                        // 签名通过，解密
                        String decrypted = ECDHKeyGen.decrypt(key, payload);
                        byte[] decryptedBytes = decrypted.getBytes(StandardCharsets.UTF_8);

                        ServerHttpRequest mutated = new ServerHttpRequestDecorator(request) {
                            @Override
                            public Flux<DataBuffer> getBody() {
                                return Flux.just(exchange.getResponse().bufferFactory().wrap(decryptedBytes));
                            }

                            @Override
                            public HttpHeaders getHeaders() {
                                HttpHeaders h = new HttpHeaders();
                                h.putAll(super.getHeaders());
                                h.setContentLength(decryptedBytes.length);
                                h.set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
                                return h;
                            }
                        };
                        logger.info("请求体解密成功: " + decrypted);
                        return Mono.just(exchange.mutate().request(mutated).build());
                    } catch (Exception e) {
                        logger.info("请求体解密失败: {}", e.getMessage());
                        return writeResponse(exchange, HttpStatus.OK,
                                ApiResponse.bussiness_error("请求体解密失败"))
                                .then(Mono.<ServerWebExchange>empty());
                    }
                });
    }

    // ======================== 加密响应体 ========================

    private ServerWebExchange wrapEncryptedResponse(ServerWebExchange exchange, SecretKey key) {
        ServerHttpResponse original = exchange.getResponse();
        DataBufferFactory factory = original.bufferFactory();

        ServerHttpResponseDecorator decorated = new ServerHttpResponseDecorator(original) {
            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                return DataBufferUtils.join(Flux.from(body))
                        .flatMap(dataBuffer -> {
                            byte[] responseBytes = new byte[dataBuffer.readableByteCount()];
                            dataBuffer.read(responseBytes);
                            DataBufferUtils.release(dataBuffer);

                            try {
                                String plain = new String(responseBytes, StandardCharsets.UTF_8);
                                String encrypted = ECDHKeyGen.encrypt(key, plain);
                                byte[] encBytes = encrypted.getBytes(StandardCharsets.UTF_8);

                                getHeaders().setContentLength(encBytes.length);
                                getHeaders().set(HttpHeaders.CONTENT_TYPE, "text/plain;charset=UTF-8");
                                return super.writeWith(Mono.just(factory.wrap(encBytes)));
                            } catch (Exception e) {
                                logger.info("响应体加密失败: {}", e.getMessage());
                                return super.writeWith(Mono.just(factory.wrap(responseBytes)));
                            }
                        });
            }
        };

        return exchange.mutate().response(decorated).build();
    }

    // ======================== 工具方法 ========================

    /**
     * 用 ApiResponse 封装写出错误响应
     */
    private <T> Mono<Void> writeResponse(ServerWebExchange exchange, HttpStatus status, ApiResponse<T> apiResponse) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().set(HttpHeaders.CONTENT_TYPE, "application/json;charset=UTF-8");
        try {
            byte[] body = MAPPER.writeValueAsBytes(apiResponse);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
        } catch (JsonProcessingException | RuntimeException e) {
            logger.info("写出错误响应失败: {}", e.getMessage());
            byte[] fallback = JacksonUtil.toJsonString(ApiResponse.error("未知错误")).orElse("{\"success\":false,\"code\":500,\"data\":\"未知错误\"}")
                    .getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(fallback)));
        }
    }
}
