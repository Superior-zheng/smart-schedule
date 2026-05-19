package com.zhangwenzheng.smartschedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhangwenzheng.smartschedule.auth.AuthInterceptor;
import com.zhangwenzheng.smartschedule.entity.VisionItem;
import com.zhangwenzheng.smartschedule.mapper.VisionItemMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vision")
public class VisionController {

    @Autowired
    private VisionItemMapper visionItemMapper;

    @GetMapping("/items")
    public List<VisionItem> getItems(@RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        return visionItemMapper.selectList(new QueryWrapper<VisionItem>().eq("owner", username).orderByDesc("create_time"));
    }

    @PostMapping("/items")
    public Map<String, Object> addItem(@RequestBody VisionItem item,
                                       @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        if (item.getName() == null || item.getName().trim().isEmpty()) {
            res.put("ok", false);
            res.put("message", "名称不能为空");
            return res;
        }
        if (item.getTargetDate() == null) {
            res.put("ok", false);
            res.put("message", "日期不能为空");
            return res;
        }

        item.setOwner(username);
        if (item.getMode() == null) item.setMode("countdown");
        if (item.getRepeatMode() == null) item.setRepeatMode("none");
        visionItemMapper.insert(item);

        res.put("ok", true);
        res.put("data", item);
        return res;
    }

    @PutMapping("/items")
    public Map<String, Object> updateItem(@RequestBody VisionItem item,
                                          @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        item.setOwner(username);
        int rows = visionItemMapper.update(item,
                new QueryWrapper<VisionItem>().eq("id", item.getId()).eq("owner", username));
        if (rows > 0) {
            res.put("ok", true);
            res.put("message", "更新成功");
        } else {
            res.put("ok", false);
            res.put("message", "未找到该记录");
        }
        return res;
    }

    @DeleteMapping("/items/{id}")
    public Map<String, Object> deleteItem(@PathVariable Long id,
                                          @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        visionItemMapper.delete(new QueryWrapper<VisionItem>().eq("id", id).eq("owner", username));
        res.put("ok", true);
        res.put("message", "删除成功");
        return res;
    }
}
