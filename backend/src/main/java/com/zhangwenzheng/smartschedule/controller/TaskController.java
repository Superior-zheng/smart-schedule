package com.zhangwenzheng.smartschedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.zhangwenzheng.smartschedule.auth.AuthInterceptor;
import com.zhangwenzheng.smartschedule.entity.Task;
import com.zhangwenzheng.smartschedule.entity.User;
import com.zhangwenzheng.smartschedule.mapper.TaskMapper;
import com.zhangwenzheng.smartschedule.mapper.UserMapper;
import com.zhangwenzheng.smartschedule.service.DeepSeekService;
import com.zhangwenzheng.smartschedule.service.HistoryEventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 任务管理核心控制器
 * 负责接收前端请求并调度 Service 和 Mapper 处理数据
 */
@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final int MAX_TITLE_LENGTH = 200;
    private static final int MAX_CONTENT_LENGTH = 2000;

    @Autowired
    private DeepSeekService deepSeekService;

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private HistoryEventService historyEventService;

    private boolean isAdmin(String username) {
        if (username == null) return false;
        User u = userMapper.selectById(username);
        return u != null && u.getRole() != null && u.getRole() == 1;
    }

    private boolean isOverdue24h(Task task) {
        if (task == null) return false;
        if (task.getStatus() == null || task.getStatus() != Task.STATUS_TODO) return false;
        LocalDateTime deadline = task.getDeadline();
        if (deadline == null) return false;
        return Duration.between(deadline, LocalDateTime.now()).toHours() >= 24;
    }

    private void syncOverdueTasks(List<Task> tasks, boolean admin, String username) {
        if (tasks == null || tasks.isEmpty()) return;
        for (Task task : tasks) {
            if (!isOverdue24h(task)) continue;

            Task patch = new Task();
            patch.setStatus(Task.STATUS_OVERDUE);

            if (admin) {
                taskMapper.update(patch, new UpdateWrapper<Task>().eq("id", task.getId()));
            } else {
                taskMapper.update(patch, new UpdateWrapper<Task>().eq("id", task.getId()).eq("owner", username));
            }
            task.setStatus(Task.STATUS_OVERDUE);
        }
    }

    /**
     * 智能解析接口：接收口语化文本，由 AI 解析并存入数据库
     * 对应请求：POST /api/tasks/ai-parse
     */
    @PostMapping("/ai-parse")
    public Task parseAndSave(@RequestBody Map<String, String> payload,
                             @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        String input = payload.get("content");
        if (input == null || input.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content 不能为空");
        }
        if (input.length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "内容长度不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }

        Task task;
        try {
            task = deepSeekService.parseTextToTask(input);
        } catch (IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, ex.getMessage());
        }

        // 验证AI解析结果
        if (task.getTitle() != null && task.getTitle().length() > MAX_TITLE_LENGTH) {
            task.setTitle(task.getTitle().substring(0, MAX_TITLE_LENGTH));
        }
        if (task.getContent() != null && task.getContent().length() > MAX_CONTENT_LENGTH) {
            task.setContent(task.getContent().substring(0, MAX_CONTENT_LENGTH));
        }

        task.setOwner(username);
        if (task.getStatus() == null) {
            task.setStatus(Task.STATUS_TODO);
        }

        taskMapper.insert(task);
        System.out.println("数据持久化成功，标题: " + task.getTitle());
        return task;
    }

    /**
     * 手动创建任务接口
     * 对应请求：POST /api/tasks
     */
    @PostMapping
    public Task createTask(@RequestBody Task task,
                           @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        if (task.getTitle() == null || task.getTitle().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务标题不能为空");
        }
        if (task.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题长度不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
        if (task.getContent() != null && task.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "内容长度不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }

        task.setOwner(username);
        if (task.getStatus() == null) {
            task.setStatus(Task.STATUS_TODO);
        }
        if (task.getTaskType() == null) {
            task.setTaskType(Task.TYPE_REMINDER);
        }
        // 提醒事项不需要 durationMinutes
        if (task.getTaskType() == Task.TYPE_REMINDER) {
            task.setDurationMinutes(null);
        }

        task.setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);
        System.out.println("手动创建任务成功，标题: " + task.getTitle());
        return task;
    }

    /**
     * 查询接口：获取数据库中所有的日程任务
     * 对应请求：GET /api/tasks/all
     */
    @GetMapping("/all")
    public List<Task> getAllTasks(@RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        boolean admin = isAdmin(username);
        List<Task> tasks;
        if (admin) {
            tasks = taskMapper.selectList(null);
        } else {
            tasks = taskMapper.selectList(new QueryWrapper<Task>().eq("owner", username));
        }
        syncOverdueTasks(tasks, admin, username);
        return tasks;
    }

    /**
     * 删除接口：根据 ID 物理删除任务
     * 对应请求：DELETE /api/tasks/{id}
     */
    @DeleteMapping("/{id}")
    public String deleteTask(@PathVariable Long id,
                             @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        if (isAdmin(username)) {
            taskMapper.deleteById(id);
            return "删除成功";
        }
        taskMapper.delete(new QueryWrapper<Task>().eq("id", id).eq("owner", username));
        return "删除成功";
    }

    /**
     * 更新接口：修改已有的日程任务信息
     * 对应请求：PUT /api/tasks
     */
    @PutMapping
    public String updateTask(@RequestBody Task task,
                             @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        if (task.getId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务ID不能为空");
        }
        if (task.getTitle() != null && task.getTitle().length() > MAX_TITLE_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "标题长度不能超过 " + MAX_TITLE_LENGTH + " 个字符");
        }
        if (task.getContent() != null && task.getContent().length() > MAX_CONTENT_LENGTH) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "内容长度不能超过 " + MAX_CONTENT_LENGTH + " 个字符");
        }

        if (isAdmin(username)) {
            taskMapper.updateById(task);
            return "修改成功";
        }
        task.setOwner(username);
        taskMapper.update(task, new UpdateWrapper<Task>().eq("id", task.getId()).eq("owner", username));
        return "修改成功";
    }

    @GetMapping("/morning-report")
    public String getMorningReport(@RequestParam int taskCount) {
        String systemPrompt = "你是一个贴心的日程助手，语气要幽默、像好哥们。";
        String userContent = String.format("我现在有 %d 个待办任务。请写一句 20 字以内的每日寄语，鼓励或调侃一下我。", taskCount);

        return deepSeekService.callAI(systemPrompt, userContent, false);
    }

    @GetMapping("/history-today")
    public Map<String, Object> getHistoryToday() {
        return historyEventService.getEventOfDay(LocalDate.now());
    }
}
