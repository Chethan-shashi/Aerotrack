package com.aerotrack.backend.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Minimal JSON Web Token (RFC 7519) implementation using HMAC-SHA256 (HS256).
 * Implemented with the JDK only, so no extra JWT library is required.
 * <p>
 * Token = base64url(header) . base64url(payload) . base64url(HMAC_SHA256(secret, header.payload))
 */
@Service
public class JwtService {

    public record Claims(String username, String role, Instant expiresAt) {}

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64D = Base64.getUrlDecoder();
    private static final String HEADER = B64.encodeToString("{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper mapper = new ObjectMapper();
    private final byte[] secret;
    private final long ttlMinutes;

    public JwtService(@Value("${aerotrack.jwt.secret}") String secret,
                      @Value("${aerotrack.jwt.ttl-minutes:480}") long ttlMinutes) {
        if (secret.length() < 32) {
            throw new IllegalStateException("aerotrack.jwt.secret must be at least 32 characters");
        }
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlMinutes = ttlMinutes;
    }

    public String issue(String username, String role) {
        try {
            Instant now = Instant.now();
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", username);
            payload.put("role", role);
            payload.put("iat", now.getEpochSecond());
            payload.put("exp", now.plusSeconds(ttlMinutes * 60).getEpochSecond());

            String body = B64.encodeToString(mapper.writeValueAsBytes(payload));
            String unsigned = HEADER + "." + body;
            return unsigned + "." + B64.encodeToString(sign(unsigned));
        } catch (Exception e) {
            throw new IllegalStateException("Could not create token", e);
        }
    }

    /** Returns the claims if the signature is valid and the token has not expired, otherwise null. */
    public Claims verify(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !HEADER.equals(parts[0])) return null;

            byte[] expected = sign(parts[0] + "." + parts[1]);
            // Constant-time comparison prevents timing attacks on the signature
            if (!MessageDigest.isEqual(expected, B64D.decode(parts[2]))) return null;

            Map<String, Object> payload = mapper.readValue(B64D.decode(parts[1]), new TypeReference<>() {});
            Instant exp = Instant.ofEpochSecond(((Number) payload.get("exp")).longValue());
            if (exp.isBefore(Instant.now())) return null;

            return new Claims((String) payload.get("sub"), (String) payload.get("role"), exp);
        } catch (Exception e) {
            return null;
        }
    }

    public long getTtlMinutes() {
        return ttlMinutes;
    }

    private byte[] sign(String data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
    }
}

