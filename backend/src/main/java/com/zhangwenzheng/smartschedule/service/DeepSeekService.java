package com.zhangwenzheng.smartschedule.service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.zhangwenzheng.smartschedule.entity.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.url:https://api.deepseek.com/chat/completions}")
    private String apiUrl;

    @Value("${deepseek.enabled:true}")
    private boolean deepSeekEnabled;

    public Task parseTextToTask(String userInput) {
        if (!deepSeekEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DeepSeek not enabled or API key not configured, using local parsing");
            return parseTaskLocally(userInput);
        }

        String prompt = "You are a schedule management assistant. Extract task information from user input and return it" +
                " strictly in JSON format. " +
                "JSON fields include: " +
                "title (task title), " +
                "content (detailed description), " +
                "deadline (start time, format yyyy-MM-dd HH:mm:ss, can be empty), " +
                "priority (1-3, only for reminder tasks), " +
                "taskType (1-reminder, 2-timed), " +
                "durationMinutes (planned duration in minutes, only for timed tasks). " +
                "Rules: " +
                "1) Task types classification: " +
                "   - Timed tasks (taskType=2): ONLY activities that require sitting in front of a computer/device for focused work." +
                " Examples: online courses, coding, writing papers, watching video tutorials, doing homework on computer, programming practice," +
                " digital reading. " +
                "   - Reminder tasks (taskType=1): Everything else including: " +
                "     * Activities requiring physical presence but not computer (exercise, gym, running, yoga, sports) " +
                "     * Offline activities (reading physical books, memorizing vocabulary with paper, practicing instruments) " +
                "     * Appointments and meetings (even if long duration) " +
                "     * Daily life tasks (grocery shopping, cooking, cleaning, medication) " +
                "     * Social activities (phone calls, replying messages, attending events) " +
                "     * Any activity where you don't need to sit at a computer " +
                "2) For timed tasks (taskType=2): Set durationMinutes (estimated time needed at computer). Priority is not required. " +
                "3) For reminder tasks (taskType=1): Set priority (1=low, 2=medium, 3=high), durationMinutes should be null. " +
                "4) Priority rules for reminder tasks: " +
                "   - High (3): Exams, interviews, deadlines, flights, trains, important meetings, urgent matters. " +
                "   - Medium (2): Regular appointments, scheduled activities. " +
                "   - Low (1): Optional tasks, flexible timing items. " +
                "5) Duration estimation for timed tasks (computer-based): If not specified, estimate based on activity type" +
                " (coding=60min, online course=40min, writing=45min, etc.). " +
                "6) Key principle: If the task can be done without a computer, it's a reminder task, NOT a timed task. " +
                "7) Output only JSON, no extra explanation. " +
                "Current system time: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        try {
            String aiResponse = callAI(prompt, userInput, true);
            Task task = JSON.parseObject(aiResponse, Task.class);
            if (task == null) {
                log.warn("AI returned null, using local parsing: {}", aiResponse);
                return parseTaskLocally(userInput);
            }
            applyDurationDefaults(task, userInput);
            return task;
        } catch (Exception e) {
            log.error("AI parsing failed, using local fallback: {}", e.getMessage(), e);
            return parseTaskLocally(userInput);
        }
    }

    private Task parseTaskLocally(String input) {
        Task task = new Task();
        task.setTitle(input.length() > 50 ? input.substring(0, 50) : input);
        task.setContent(input);
        task.setStatus(Task.STATUS_TODO);
        task.setPriority(2);
        task.setTaskType(Task.TYPE_REMINDER);

        String lowerInput = input.toLowerCase();

        if (lowerInput.contains("exam") || lowerInput.contains("interview") || lowerInput.contains("urgent") || lowerInput.contains("must complete today")) {
            task.setPriority(3);
        } else if (lowerInput.contains("grocery") || lowerInput.contains("medicine") || lowerInput.contains("reply") || lowerInput.contains("check in")) {
            task.setPriority(1);
        }

        // Only computer-based learning activities should be timed tasks
        if (lowerInput.contains("online course") || lowerInput.contains("coding") || lowerInput.contains("programming") || 
            lowerInput.contains("computer homework") || lowerInput.contains("video tutorial")) {
            task.setTaskType(Task.TYPE_TIMED);
            task.setDurationMinutes(40);
        }

        return task;
    }

    private void applyDurationDefaults(Task task, String userInput) {
        if (task == null) return;

        String text = ((task.getTitle() == null ? "" : task.getTitle()) + " " +
                (task.getContent() == null ? "" : task.getContent()) + " " +
                (userInput == null ? "" : userInput)).toLowerCase();

        if (task.getTaskType() == null) {
            // Only computer-based activities should be timed tasks
            String[] timedKeywords = new String[]{"online course", "coding", "programming", "computer homework", "video tutorial", "digital reading"};
            boolean isTimed = false;
            for (String k : timedKeywords) {
                if (text.contains(k)) {
                    isTimed = true;
                    break;
                }
            }
            task.setTaskType(isTimed ? Task.TYPE_TIMED : Task.TYPE_REMINDER);
        }

        Integer duration = task.getDurationMinutes();
        if (duration != null && duration > 0) return;

        String[] instantKeywords = new String[]{
                "water", "reply message", "reply", "confirm", "click", "check in", "sign in", "grocery", "medicine", "game"
        };
        for (String k : instantKeywords) {
            if (text.contains(k)) {
                task.setDurationMinutes(null);
                if (task.getTaskType() == null) task.setTaskType(Task.TYPE_REMINDER);
                return;
            }
        }

        if (task.getTaskType() != null && task.getTaskType() == Task.TYPE_REMINDER) {
            task.setDurationMinutes(null);
            return;
        }

        // For timed tasks, estimate duration if not set
        Integer p = task.getPriority();
        int estimated;
        if (p != null && p >= 3) estimated = 60;
        else if (p != null && p == 2) estimated = 40;
        else estimated = 25;
        task.setDurationMinutes(estimated);
    }

    public String callAI(String systemPrompt, String userContent, boolean isJson) {
        if (!deepSeekEnabled || apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("DeepSeek not enabled, returning default text");
            return getDefaultResponse(userContent);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("model", "deepseek-chat");
        body.put("messages", new Object[]{
                new HashMap<String, String>() {{ put("role", "system"); put("content", systemPrompt); }},
                new HashMap<String, String>() {{ put("role", "user"); put("content", userContent); }}
        });

        if (isJson) {
            body.put("response_format", new HashMap<String, String>() {{ put("type", "json_object"); }});
        }

        try {
            log.info("Calling DeepSeek API: {}", apiUrl);
            String response = com.dtflys.forest.Forest.post(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .contentTypeJson()
                    .connectTimeout(30000).readTimeout(30000)
                    .addBody(JSON.toJSONString(body))
                    .executeAsString();

            log.debug("DeepSeek response: {}", response);

            if (response == null || response.trim().isEmpty()) {
                log.error("DeepSeek returned empty response");
                return getDefaultResponse(userContent);
            }

            JSONObject jsonResponse = JSON.parseObject(response);

            if (jsonResponse.containsKey("error")) {
                String errorMsg = jsonResponse.getJSONObject("error").getString("message");
                log.error("DeepSeek API returned error: {}", errorMsg);
                return getDefaultResponse(userContent);
            }

            if (!jsonResponse.containsKey("choices") || jsonResponse.getJSONArray("choices").isEmpty()) {
                log.error("DeepSeek response missing choices field: {}", response);
                return getDefaultResponse(userContent);
            }

            String content = jsonResponse.getJSONArray("choices")
                    .getJSONObject(0).getJSONObject("message").getString("content");

            if (content == null || content.isBlank()) {
                log.error("AI returned empty content, response: {}", response);
                return getDefaultResponse(userContent);
            }

            return content;
        } catch (IllegalStateException e) {
            log.error("AI call state exception: {}", e.getMessage());
            return getDefaultResponse(userContent);
        } catch (Exception e) {
            log.error("AI call failed: {}", e.getMessage(), e);
            return getDefaultResponse(userContent);
        }
    }

    private String getDefaultResponse(String userContent) {
        return "Today also needs steady progress, don't let plans stop at ideas.";
    }
}
