package com.itheima.dormrepairsystemspringboot.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtUtil {

    private static final String SECRET_KEY = "your_secret_key_1234567890abcdefghijklmnopqrstuvwxyz";
    private static final long EXPIRE_TIME = 1000 * 60 * 60 * 24;  //过期时间24小时，单位（毫秒）

    //SecretKey - java加密包里的密钥类型
    //SECRET_KEY由字符类型变成字节类型后再通过hmacshakeyfor生成标准密钥存储进KEY中
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

    //传入用户数据（Map），返回一段token字符串
    public static String generateToken(Map<String, Object> claims) {      //Map<String,Object> claims - 对象、键名、值+变量名
        return Jwts.builder()
                //1.header - Jwts.builder自动创建
                //2.payload - 用户数据
                .setClaims(claims)    //具体用户数据
                .setIssuedAt(new Date())  //生成时间
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRE_TIME))  //过期时间，生成时间+生效时间（24h）
                //3.signature
                .signWith(KEY, SignatureAlgorithm.HS256)
                //4.总和
                .compact();
    }

    //传入token字符，解析后校验密钥，接收用户信息
    public static Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(KEY)   //选择校验的标准密钥
                .build()   //准备工具
                .parseClaimsJws(token)  //解析token内容，校验signature和过期时间
                .getBody();        //把token里的payload接收给后端
        //如果token不对直接报错，对的话返回claims（id、account、role）
    }

    //判断token信息期没有
    public static boolean isExpired(String token) {
        return parseToken(token).getExpiration().before(new Date());
    }
}