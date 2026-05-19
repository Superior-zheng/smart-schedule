package com.zhangwenzheng.smartschedule;

import com.zhangwenzheng.smartschedule.entity.Task;
import com.zhangwenzheng.smartschedule.mapper.TaskMapper;
import com.zhangwenzheng.smartschedule.service.DeepSeekService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmartScheduleApplicationTests {

    @Autowired
    private TaskMapper taskMapper;

    @Autowired
    private DeepSeekService deepSeekService; // 注入 AI 服务

    // 之前的数据库测试方法
    @Test
    void testInsert() {
        Task task = new Task();
        task.setTitle("手动录入测试");
        taskMapper.insert(task);
        System.out.println("数据库手动存取 OK！");
    }

    // AI 解析并自动入库测试
    @Test
    void testAiParseAndSave() {
        // 1. 模拟用户输入
        String input = "明天下午两点在泰山学院校本部开班会";

        // 2. 调用刚写好的 AI 服务
        Task aiTask = deepSeekService.parseTextToTask(input);

        if (aiTask != null) {
            System.out.println("AI 解析出的标题: " + aiTask.getTitle());
            System.out.println("AI 解析出的时间: " + aiTask.getDeadline());

            // 3. 将 AI 的结果保存到数据库（实现开题报告中的闭环）[cite: 4, 7]
            taskMapper.insert(aiTask);
            System.out.println("AI 任务已成功存入数据库，ID: " + aiTask.getId());
        } else {
            System.err.println("AI 解析失败，请检查 API Key 或网络！");
        }
    }
}