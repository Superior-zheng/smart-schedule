package com.zhangwenzheng.smartschedule.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class TaskTest {

    @Test
    @DisplayName("任务状态常量应正确设置")
    void testStatusConstants() {
        assertEquals(0, Task.STATUS_TODO);
        assertEquals(1, Task.STATUS_DONE);
        assertEquals(2, Task.STATUS_OVERDUE);
    }

    @Test
    @DisplayName("任务类型常量应正确设置")
    void testTypeConstants() {
        assertEquals(1, Task.TYPE_REMINDER);
        assertEquals(2, Task.TYPE_LEARNING);
    }

    @Test
    @DisplayName("任务实体应正确设置和获取属性")
    void testTaskProperties() {
        Task task = new Task();
        LocalDateTime deadline = LocalDateTime.of(2026, 4, 20, 14, 0);

        task.setId(1L);
        task.setTitle("测试任务");
        task.setContent("这是一个测试任务内容");
        task.setPriority(3);
        task.setStatus(Task.STATUS_TODO);
        task.setOwner("testuser");
        task.setDeadline(deadline);
        task.setDurationMinutes(30);
        task.setTaskType(Task.TYPE_LEARNING);
        task.setCreateTime(LocalDateTime.now());

        assertEquals(1L, task.getId());
        assertEquals("测试任务", task.getTitle());
        assertEquals("这是一个测试任务内容", task.getContent());
        assertEquals(3, task.getPriority());
        assertEquals(Task.STATUS_TODO, task.getStatus());
        assertEquals("testuser", task.getOwner());
        assertEquals(deadline, task.getDeadline());
        assertEquals(30, task.getDurationMinutes());
        assertEquals(Task.TYPE_LEARNING, task.getTaskType());
        assertNotNull(task.getCreateTime());
    }
}
