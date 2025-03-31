package com.fongmi.android.tv.utils;

import android.util.Log;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.Claim;

import java.util.Date;
import java.util.Map;

public class JwtManager {
    private static final String TAG = "JwtManager";
    private static final String JWT_SECRET = "354cBbW8Zi1zqnCCvI5aPKpdAv6c84n8"; // 应该从服务器获取或配置
    private static final long TOKEN_EXPIRY = 30 * 24 * 60 * 60 * 1000L; // 30天

    private static JwtManager instance;
    private final Algorithm algorithm;

    private JwtManager() {
        algorithm = Algorithm.HMAC256(JWT_SECRET);
    }

    public static JwtManager getInstance() {
        if (instance == null) {
            instance = new JwtManager();
        }
        return instance;
    }

    public String generateToken(String deviceId) {
        try {
            return JWT.create()
                    .withClaim("deviceId", deviceId)
                    .withClaim("type", "tv")
                    .withIssuedAt(new Date())
                    .withExpiresAt(new Date(System.currentTimeMillis() + TOKEN_EXPIRY))
                    .sign(algorithm);
        } catch (Exception e) {
            Log.e(TAG, "Token generation failed", e);
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            JWT.require(algorithm)
                .build()
                .verify(token);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Token validation failed", e);
            return false;
        }
    }

    public Map<String, Claim> getClaims(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaims();
        } catch (Exception e) {
            Log.e(TAG, "Failed to get claims from token", e);
            return null;
        }
    }

    public boolean hasPermission(String token, String permission) {
        try {
            Map<String, Claim> claims = getClaims(token);
            if (claims == null) return false;

            // 检查设备类型权限
            Claim typeClaim = claims.get("type");
            if (typeClaim != null && "tv".equals(typeClaim.asString())) {
                // TV设备有所有权限
                return true;
            }

            // 检查特定权限
            Claim permissionClaim = claims.get(permission);
            return permissionClaim != null && permissionClaim.asBoolean();
        } catch (Exception e) {
            Log.e(TAG, "Permission check failed", e);
            return false;
        }
    }
} 