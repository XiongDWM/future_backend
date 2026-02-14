package com.xiongdwm.future_backend.resource;

import com.xiongdwm.future_backend.bo.ApiResponse;
import com.xiongdwm.future_backend.utils.cache.CacheHandler;
import com.xiongdwm.future_backend.utils.cache.LRUCache;
import com.xiongdwm.future_backend.utils.ecc.CryptoSessionConfig;
import com.xiongdwm.future_backend.utils.ecc.ECDHKeyGen;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import javax.crypto.SecretKey;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Base64;
import java.util.UUID;

@RestController
public class CryptoController {

    @Autowired
    private CacheHandler cacheHandler;

    /**
     * 单次握手接口
     * <p>
     * 请求:
     *   Header  X-Client-Key: {clientPublicKey base64}
     *   Body:   任意（被忽略）
     * <p>
     * 响应:
     *   Header  X-Server-Key: {serverPublicKey base64}
     *   Header  X-Session-Id: {sessionId}
     *   Body:   伪装的无意义内容
     */
    @PostMapping("/crypto/handshake")
    public ApiResponse<String> handshake(ServerWebExchange exchange) {
        String clientKeyBase64 = exchange.getRequest().getHeaders().getFirst("X-Client-Key");
        System.out.println("收到握手请求，客户端公钥: " + clientKeyBase64);
        if (clientKeyBase64 == null || clientKeyBase64.isBlank()) {
            return ApiResponse.bussiness_error("something went wrong");
        }
        try {
            // 后端生成密钥对
            KeyPair serverKeyPair = ECDHKeyGen.generateKeyPair();

            // 用客户端公钥 + 服务端私钥派生共享密钥
            PublicKey clientPubKey = ECDHKeyGen.decodePublicKey(clientKeyBase64);
            SecretKey sharedKey = ECDHKeyGen.deriveSharedSecret(serverKeyPair.getPrivate(), clientPubKey);

            // 生成 sessionId，存入 km 缓存
            String sessionId = UUID.randomUUID().toString().replace("-", "");
            LRUCache<String, SecretKey> km = cacheHandler.getCache(CryptoSessionConfig.CACHE_KM);
            km.put(sessionId, sharedKey);

            // 服务端公钥和 sessionId 藏在响应 header 里
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().set("X-Server-Key", ECDHKeyGen.encodePublicKey(serverKeyPair.getPublic()));
            response.getHeaders().set("X-Session-Id", sessionId);

            // body 返回伪装内容
            return ApiResponse.success(Base64.getEncoder().encodeToString("hello_world_rookie".getBytes()));
        } catch (Exception e) {
            return ApiResponse.error("handshake failed");
        }
    }
}
