package com.zhangwenzheng.smartschedule.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId
    private String username;

    private String password;

    private String nickname;

    private LocalDateTime createTime;

    /** 角色：0-普通用户, 1-管理员 */
    private Integer role;
}
