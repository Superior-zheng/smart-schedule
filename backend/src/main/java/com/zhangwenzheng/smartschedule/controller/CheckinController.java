package com.zhangwenzheng.smartschedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhangwenzheng.smartschedule.auth.AuthInterceptor;
import com.zhangwenzheng.smartschedule.entity.CheckinRecord;
import com.zhangwenzheng.smartschedule.entity.Habit;
import com.zhangwenzheng.smartschedule.mapper.CheckinRecordMapper;
import com.zhangwenzheng.smartschedule.mapper.HabitMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    @Autowired
    private HabitMapper habitMapper;

    @Autowired
    private CheckinRecordMapper checkinRecordMapper;

    @GetMapping("/habits")
    public List<Habit> getHabits(@RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        return habitMapper.selectList(new QueryWrapper<Habit>().eq("owner", username).orderByDesc("create_time"));
    }

    @PostMapping("/habits")
    public Map<String, Object> addHabit(@RequestBody Habit habit,
                                        @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        if (habit.getName() == null || habit.getName().trim().isEmpty()) {
            res.put("ok", false);
            res.put("message", "名称不能为空");
            return res;
        }
        if (habit.getName().length() > 100) {
            res.put("ok", false);
            res.put("message", "名称不能超过100个字符");
            return res;
        }

        habit.setOwner(username);
        habitMapper.insert(habit);
        res.put("ok", true);
        res.put("data", habit);
        return res;
    }

    @DeleteMapping("/habits/{id}")
    public Map<String, Object> deleteHabit(@PathVariable Long id,
                                           @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        habitMapper.delete(new QueryWrapper<Habit>().eq("id", id).eq("owner", username));
        checkinRecordMapper.delete(new QueryWrapper<CheckinRecord>().eq("habit_id", id).eq("owner", username));
        res.put("ok", true);
        res.put("message", "删除成功");
        return res;
    }

    @GetMapping("/records")
    public Map<String, List<Long>> getRecords(@RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        List<CheckinRecord> records = checkinRecordMapper.selectList(
                new QueryWrapper<CheckinRecord>().eq("owner", username));

        Map<String, List<Long>> result = new HashMap<>();
        for (CheckinRecord r : records) {
            String key = r.getCheckinDate().toString();
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(r.getHabitId());
        }
        return result;
    }

    @PostMapping("/records")
    public Map<String, Object> doCheckin(@RequestBody Map<String, Object> payload,
                                         @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        Long habitId = Long.valueOf(payload.get("habitId").toString());
        String dateStr = (String) payload.get("date");
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

        CheckinRecord existing = checkinRecordMapper.selectOne(
                new QueryWrapper<CheckinRecord>().eq("habit_id", habitId).eq("checkin_date", date).eq("owner", username));

        if (existing != null) {
            res.put("ok", true);
            res.put("message", "今天已打卡");
            return res;
        }

        CheckinRecord record = new CheckinRecord();
        record.setHabitId(habitId);
        record.setOwner(username);
        record.setCheckinDate(date);
        checkinRecordMapper.insert(record);

        res.put("ok", true);
        res.put("message", "打卡成功");
        return res;
    }

    @DeleteMapping("/records")
    public Map<String, Object> undoCheckin(@RequestBody Map<String, Object> payload,
                                           @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        Long habitId = Long.valueOf(payload.get("habitId").toString());
        String dateStr = (String) payload.get("date");
        LocalDate date = dateStr != null ? LocalDate.parse(dateStr) : LocalDate.now();

        checkinRecordMapper.delete(
                new QueryWrapper<CheckinRecord>().eq("habit_id", habitId).eq("checkin_date", date).eq("owner", username));

        res.put("ok", true);
        res.put("message", "已取消打卡");
        return res;
    }
}
