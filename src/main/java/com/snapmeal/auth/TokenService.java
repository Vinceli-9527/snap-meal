package com.snapmeal.auth;

import com.snapmeal.common.UnauthorizedException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class TokenService {
    private static final long TTL_SECONDS = 8 * 60 * 60;
    private static final String INVALID = "登录状态无效或已过期";
    private final byte[] secret;
    private final JdbcTemplate jdbc;
    private final RedisTemplate<String, String> redis;
    private final boolean redisMode;
    private final ConcurrentHashMap<String, SessionRecord> memory = new ConcurrentHashMap<>();

    public TokenService(@Value("${sky.security.token-secret}") String secret,
                        @Value("${sky.integrations.redis-mode:memory}") String redisMode,
                        JdbcTemplate jdbc,
                        ObjectProvider<RedisTemplate<String, String>> redis) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.jdbc = jdbc;
        this.redis = redis.getIfAvailable();
        this.redisMode = "redis".equalsIgnoreCase(redisMode);
    }

    public String issue(long subjectId, String role) {
        long expires = Instant.now().plusSeconds(TTL_SECONDS).getEpochSecond();
        String payload = subjectId + ":" + role + ":" + expires + ":" + System.nanoTime();
        String token = encode(payload.getBytes(StandardCharsets.UTF_8)) + "." + encode(sign(payload));
        String tokenHash = hash(token);
        jdbc.update("insert into auth_session(token_hash,subject_id,role,expires_at,revoked) values(?,?,?,?,0)",
                tokenHash, subjectId, role, java.sql.Timestamp.from(Instant.ofEpochSecond(expires)));
        writeMemory(tokenHash, subjectId, role, expires);
        if (redisMode) writeRedis(tokenHash, subjectId, role, expires);
        return token;
    }

    public Session verify(String token, String role) {
        try {
            ParsedToken parsed = parse(token, role);
            Session redisSession = readRedis(parsed.tokenHash, role);
            if (redisSession != null) return redisSession;
            Session memorySession = readMemory(parsed.tokenHash, role);
            if (memorySession != null) return memorySession;
            Integer active = jdbc.queryForObject("select count(*) from auth_session where token_hash=? and subject_id=? and role=? and revoked=0 and expires_at>current_timestamp",
                    Integer.class, parsed.tokenHash, parsed.subjectId, role);
            if (active == null || active == 0) throw new UnauthorizedException(INVALID);
            return new Session(parsed.subjectId, parsed.role, Instant.ofEpochSecond(parsed.expires));
        } catch (UnauthorizedException e) {
            throw e;
        } catch (Exception e) {
            throw new UnauthorizedException(INVALID);
        }
    }

    public void revoke(String token) {
        if (token == null || token.trim().isEmpty()) return;
        String tokenHash = hash(token);
        jdbc.update("update auth_session set revoked=1 where token_hash=?", tokenHash);
        memory.remove(tokenHash);
        deleteRedis(tokenHash);
    }

    private ParsedToken parse(String token, String role) {
        if (token == null || token.trim().isEmpty()) throw new UnauthorizedException("请先登录");
        String[] parts = token.split("\\.");
        if (parts.length != 2) throw new UnauthorizedException(INVALID);
        String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        byte[] provided = Base64.getUrlDecoder().decode(parts[1]);
        if (!MessageDigest.isEqual(sign(payload), provided)) throw new UnauthorizedException(INVALID);
        String[] fields = payload.split(":");
        if (fields.length != 4) throw new UnauthorizedException(INVALID);
        long id = Long.parseLong(fields[0]);
        String actualRole = fields[1];
        long expires = Long.parseLong(fields[2]);
        if (!actualRole.equals(role) || expires < Instant.now().getEpochSecond()) throw new UnauthorizedException(INVALID);
        return new ParsedToken(hash(token), id, actualRole, expires);
    }

    private void writeRedis(String tokenHash, long subjectId, String role, long expires) {
        if (!canUseRedis()) return;
        try {
            redis.opsForValue().set(redisKey(tokenHash), subjectId + ":" + role + ":" + expires, Duration.ofSeconds(TTL_SECONDS));
        } catch (Exception ignored) {
            // Redis is optional; H2 auth_session remains the fallback store.
        }
    }

    private void writeMemory(String tokenHash, long subjectId, String role, long expires) {
        memory.put(tokenHash, new SessionRecord(subjectId, role, expires));
    }

    private Session readMemory(String tokenHash, String role) {
        SessionRecord record = memory.get(tokenHash);
        if (record == null) return null;
        if (!record.role.equals(role) || record.expires < Instant.now().getEpochSecond()) {
            memory.remove(tokenHash);
            return null;
        }
        return new Session(record.subjectId, record.role, Instant.ofEpochSecond(record.expires));
    }

    private Session readRedis(String tokenHash, String role) {
        if (!canUseRedis()) return null;
        try {
            String value = redis.opsForValue().get(redisKey(tokenHash));
            if (value == null || value.trim().isEmpty()) return null;
            String[] fields = value.split(":");
            if (fields.length != 3) return null;
            long id = Long.parseLong(fields[0]);
            String actualRole = fields[1];
            long expires = Long.parseLong(fields[2]);
            if (!actualRole.equals(role) || expires < Instant.now().getEpochSecond()) return null;
            return new Session(id, actualRole, Instant.ofEpochSecond(expires));
        } catch (Exception ignored) {
            return null;
        }
    }

    private void deleteRedis(String tokenHash) {
        if (!canUseRedis()) return;
        try {
            redis.delete(redisKey(tokenHash));
        } catch (Exception ignored) {
            // Redis is optional; revocation is still persisted in H2.
        }
    }

    private boolean canUseRedis() {
        return redisMode && redis != null;
    }

    private String redisKey(String tokenHash) {
        return "snap-meal:auth:token:" + tokenHash;
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("令牌签名失败", e);
        }
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private String hash(String token) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder();
            for (byte b : bytes) value.append(String.format("%02x", b));
            return value.toString();
        } catch (Exception e) {
            throw new IllegalStateException("令牌摘要失败", e);
        }
    }

    private static class ParsedToken {
        private final String tokenHash;
        private final long subjectId;
        private final String role;
        private final long expires;

        private ParsedToken(String tokenHash, long subjectId, String role, long expires) {
            this.tokenHash = tokenHash;
            this.subjectId = subjectId;
            this.role = role;
            this.expires = expires;
        }
    }

    private static class SessionRecord {
        private final long subjectId;
        private final String role;
        private final long expires;

        private SessionRecord(long subjectId, String role, long expires) {
            this.subjectId = subjectId;
            this.role = role;
            this.expires = expires;
        }
    }

    public static class Session {
        private final long subjectId;
        private final String role;
        private final Instant expiresAt;

        Session(long id, String role, Instant expires) {
            this.subjectId = id;
            this.role = role;
            this.expiresAt = expires;
        }

        public long getSubjectId() {
            return subjectId;
        }

        public String getRole() {
            return role;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
