package com.zhangwenzheng.smartschedule.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil("aGg3N2RmZ2hqOThzZGZnaGo5OGFzZGZnaGo5OGFzZGZnaGo5OGFzZGZnaGo5OGFzZGY=", 10080);
    }

    @Test
    @DisplayName("创建Token应返回有效的JWT字符串")
    void testCreateToken() {
        String token = jwtUtil.createToken("testuser");

        assertNotNull(token);
        assertTrue(token.split("\\.").length == 3);
    }

    @Test
    @DisplayName("解析Token应返回正确的用户名")
    void testParseToken() {
        String token = jwtUtil.createToken("testuser");
        String username = jwtUtil.parseUsername(token);

        assertEquals("testuser", username);
    }

    @Test
    @DisplayName("无效Token解析应抛出异常")
    void testParseInvalidToken() {
        assertThrows(Exception.class, () -> jwtUtil.parseUsername("invalid.token.here"));
    }

    @Test
    @DisplayName("空Token解析应抛出异常")
    void testParseEmptyToken() {
        assertThrows(Exception.class, () -> jwtUtil.parseUsername(""));
    }
}

// 用于测试的反射工具类
class ReflectionTestUtils {
    public static void setField(Object target, String name, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
