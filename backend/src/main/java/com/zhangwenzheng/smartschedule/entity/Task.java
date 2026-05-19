package com.zhangwenzheng.smartschedule.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务实体类，对应数据库中的 task 表
 */
@Data // 自动帮你生成 Getter/Setter 方法
@TableName("task") // 告诉 MyBatis-Plus 这个类对应数据库的 task 表
public class Task {

    public static final int STATUS_TODO = 0;
    public static final int STATUS_DONE = 1;
    public static final int STATUS_OVERDUE = 2;

    public static final int TYPE_REMINDER = 1;  // 提醒事项
    public static final int TYPE_TIMED = 2;     // 计时事项

    @TableId(type = IdType.AUTO) // 标注主键，并且是自动增长
    private Long id;

    private String title;      // 任务标题

    private String content;    // 任务详情

    private Integer priority;  // 优先级：1低, 2中, 3高

    private Integer status;    // 状态：0待办, 1已完成, 2逾期未完成

    /** 任务归属者（登录账号 username） */
    private String owner;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime deadline; // 截止日期

    /**
     * 预计用时（分钟）。
     * 可为空：代表“瞬间/无需计时”或用户未设置。
     */
    private Integer durationMinutes;

    /** 任务类型：1-提醒事项，2-计时事项 */
    private Integer taskType;

    /** 任务创建时间 */
    private LocalDateTime createTime;
}
