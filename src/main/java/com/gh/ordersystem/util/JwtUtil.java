
package com.gh.ordersystem.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class JwtUtil {
  // 从 application.yml 读配置
  @Value("${app.jwt.secret}")
  private String secret;

  @Value("${app.jwt.expiration}")
  private Long expiration;

  // 把字符串 secret 转成加密密钥
  private SecretKey getSecretKey() {
    return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * 生成 Token
   * 
   * @param userId   用户ID
   * @param username 用户名
   */
  public String createToken(Integer userId, String username) {
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", userId);
    claims.put("username", username);

    return Jwts.builder()
        .claims(claims) // 自定义数据
        .subject(username) // 主题
        .issuedAt(new Date()) // 签发时间
        .expiration(new Date(System.currentTimeMillis() + expiration)) // 过期时间
        .signWith(getSecretKey()) // 签名
        .compact(); // 拼成字符串
  }

  /**
   * 验证并解析 Token
   * 
   * @return Claims（包含 userId、username）
   * @throws 过期/篡改会抛异常
   */
  public Claims parseToken(String token) {
    return Jwts.parser()
        .verifyWith(getSecretKey())
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }
}
