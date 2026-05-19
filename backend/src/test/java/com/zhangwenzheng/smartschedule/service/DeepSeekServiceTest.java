package com.zhangwenzheng.smartschedule.service;

import com.alibaba.fastjson2.JSON;
import com.zhangwenzheng.smartschedule.entity.Task;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class DeepSeekServiceTest {

    private DeepSeekService deepSeekService;

    @BeforeEach
    void setUp() {
        deepSeekService = new DeepSeekService();
        ReflectionTestUtils.setField(deepSeekService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(deepSeekService, "apiUrl", "https://api.deepseek.com/chat/completions");
    }

    @Test
    @DisplayName("applyDurationDefaults - 学习型任务应设置默认时长")
    void testApplyDurationDefaults_learningTask() {
        Task task = new Task();
        task.setTaskType(Task.TYPE_LEARNING);
        task.setPriority(2);

        ReflectionTestUtils.invokeMethod(deepSeekService, "applyDurationDefaults", task, "做数学卷子");

        assertNotNull(task.getDurationMinutes());
        assertEquals(40, task.getDurationMinutes());
    }

    @Test
    @DisplayName("applyDurationDefaults - 提醒型任务不应设置时长")
    void testApplyDurationDefaults_reminderTask() {
        Task task = new Task();
        task.setTaskType(Task.TYPE_REMINDER);

        ReflectionTestUtils.invokeMethod(deepSeekService, "applyDurationDefaults", task, "买菜");

        assertNull(task.getDurationMinutes());
    }

    @Test
    @DisplayName("applyDurationDefaults - 高优先级学习任务应有更长时长")
    void testApplyDurationDefaults_highPriorityTask() {
        Task task = new Task();
        task.setTaskType(Task.TYPE_LEARNING);
        task.setPriority(3);

        ReflectionTestUtils.invokeMethod(deepSeekService, "applyDurationDefaults", task, "重要考试");

        assertNotNull(task.getDurationMinutes());
        assertEquals(60, task.getDurationMinutes());
    }

    @Test
    @DisplayName("applyDurationDefaults - 瞬间任务关键词应设置为null")
    void testApplyDurationDefaults_instantTask() {
        Task task = new Task();
        task.setTaskType(null);

        ReflectionTestUtils.invokeMethod(deepSeekService, "applyDurationDefaults", task, "喝水");

        assertNull(task.getDurationMinutes());
        assertEquals(Task.TYPE_REMINDER, task.getTaskType());
    }

    @Test
    @DisplayName("applyDurationDefaults - 根据关键词自动判断任务类型")
    void testApplyDurationDefaults_autoDetectTaskType() {
        Task task = new Task();
        task.setTaskType(null);

        ReflectionTestUtils.invokeMethod(deepSeekService, "applyDurationDefaults", task, "做卷子");

        assertEquals(Task.TYPE_LEARNING, task.getTaskType());
        assertNotNull(task.getDurationMinutes());
    }
}
