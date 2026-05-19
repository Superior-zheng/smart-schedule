package com.zhangwenzheng.smartschedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhangwenzheng.smartschedule.auth.AuthInterceptor;
import com.zhangwenzheng.smartschedule.auth.JwtUtil;
import com.zhangwenzheng.smartschedule.entity.User;
import com.zhangwenzheng.smartschedule.mapper.TaskMapper;
import com.zhangwenzheng.smartschedule.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String USERNAME_REGEX = "^[0-9]{6,12}$";
    private static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,12}$";

    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthController(UserMapper userMapper, TaskMapper taskMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.taskMapper = taskMapper;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String nickname = payload.get("nickname");

        Map<String, Object> res = new HashMap<>();

        if (username == null || !username.matches(USERNAME_REGEX)) {
            res.put("ok", false);
            res.put("message", "账号必须是6-12位纯数字");
            return res;
        }
        if (password == null || !password.matches(PASSWORD_REGEX)) {
            res.put("ok", false);
            res.put("message", "密码必须包含字母和数字且6-12位");
            return res;
        }
        if (nickname != null) {
            nickname = nickname.trim();
            if (nickname.length() > 50) {
                res.put("ok", false);
                res.put("message", "昵称不能超过50个字符");
                return res;
            }
            if (nickname.isBlank()) nickname = null;
        }

        User existing = userMapper.selectById(username);
        if (existing != null) {
            res.put("ok", false);
            res.put("message", "账号已存在");
            return res;
        }

        User u = new User();
        u.setUsername(username);
        u.setPassword(encoder.encode(password));
        u.setNickname(nickname);
        u.setRole(0);
        userMapper.insert(u);

        res.put("ok", true);
        res.put("message", "注册成功");
        return res;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");

        Map<String, Object> res = new HashMap<>();

        if (username == null || !username.matches(USERNAME_REGEX)) {
            res.put("ok", false);
            res.put("message", "账号或密码错误");
            return res;
        }
        if (password == null || password.isBlank()) {
            res.put("ok", false);
            res.put("message", "账号或密码错误");
            return res;
        }

        User u = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (u == null || !encoder.matches(password, u.getPassword())) {
            res.put("ok", false);
            res.put("message", "账号或密码错误");
            return res;
        }

        String token = jwtUtil.createToken(username);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("username", username);
        data.put("role", u.getRole());
        data.put("nickname", u.getNickname());

        res.put("ok", true);
        res.put("data", data);
        return res;
    }

    @PostMapping("/change-password")
    public Map<String, Object> changePassword(
            @RequestBody Map<String, String> payload,
            @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username
    ) {
        String oldPassword = payload.get("oldPassword");
        String newPassword = payload.get("newPassword");

        Map<String, Object> res = new HashMap<>();
        if (oldPassword == null || oldPassword.isBlank()) {
            res.put("ok", false);
            res.put("message", "请输入旧密码");
            return res;
        }
        if (newPassword == null || !newPassword.matches(PASSWORD_REGEX)) {
            res.put("ok", false);
            res.put("message", "新密码必须包含字母和数字且6-12位");
            return res;
        }

        User u = userMapper.selectById(username);
        if (u == null || !encoder.matches(oldPassword, u.getPassword())) {
            res.put("ok", false);
            res.put("message", "旧密码错误");
            return res;
        }

        u.setPassword(encoder.encode(newPassword));
        userMapper.updateById(u);
        res.put("ok", true);
        res.put("message", "密码已更新");
        return res;
    }

    @PostMapping("/change-nickname")
    public Map<String, Object> changeNickname(
            @RequestBody Map<String, String> payload,
            @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username
    ) {
        Map<String, Object> res = new HashMap<>();
        User u = userMapper.selectById(username);
        if (u == null) {
            res.put("ok", false);
            res.put("message", "账号不存在");
            return res;
        }

        String nickname = payload.get("nickname");
        if (nickname != null) nickname = nickname.trim();
        if (nickname != null && nickname.length() > 50) {
            res.put("ok", false);
            res.put("message", "昵称不能超过50个字符");
            return res;
        }

        if (nickname == null || nickname.isBlank()) {
            u.setNickname(null);
        } else {
            u.setNickname(nickname);
        }
        userMapper.updateById(u);

        res.put("ok", true);
        res.put("message", "昵称已更新");
        res.put("data", Map.of("nickname", u.getNickname()));
        return res;
    }

    @PostMapping("/delete-account")
    public Map<String, Object> deleteAccount(
            @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username
    ) {
        Map<String, Object> res = new HashMap<>();

        taskMapper.delete(new QueryWrapper<com.zhangwenzheng.smartschedule.entity.Task>().eq("owner", username));
        userMapper.deleteById(username);

        res.put("ok", true);
        res.put("message", "账号已注销");
        return res;
    }

    @PostMapping("/admin/change-password")
    public Map<String, Object> adminChangePassword(
            @RequestBody Map<String, String> payload,
            @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String adminUsername
    ) {
        Map<String, Object> res = new HashMap<>();

        User admin = userMapper.selectById(adminUsername);
        if (admin == null || admin.getRole() == null || admin.getRole() != 1) {
            res.put("ok", false);
            res.put("message", "无权限");
            return res;
        }

        String username = payload.get("username");
        String newPassword = payload.get("newPassword");

        if (username == null || !username.matches(USERNAME_REGEX)) {
            res.put("ok", false);
            res.put("message", "目标账号必须是6-12位纯数字");
            return res;
        }
        if (newPassword == null || !newPassword.matches(PASSWORD_REGEX)) {
            res.put("ok", false);
            res.put("message", "新密码必须包含字母和数字且6-12位");
            return res;
        }

        User target = userMapper.selectById(username);
        if (target == null) {
            res.put("ok", false);
            res.put("message", "目标账号不存在");
            return res;
        }

        target.setPassword(encoder.encode(newPassword));
        userMapper.updateById(target);
        res.put("ok", true);
        res.put("message", "密码已更新");
        return res;
    }
}
