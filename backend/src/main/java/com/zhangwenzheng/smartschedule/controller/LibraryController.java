package com.zhangwenzheng.smartschedule.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhangwenzheng.smartschedule.auth.AuthInterceptor;
import com.zhangwenzheng.smartschedule.entity.LibraryLink;
import com.zhangwenzheng.smartschedule.mapper.LibraryLinkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
public class LibraryController {

    @Autowired
    private LibraryLinkMapper libraryLinkMapper;

    @GetMapping("/links")
    public List<LibraryLink> getLinks(@RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        return libraryLinkMapper.selectList(new QueryWrapper<LibraryLink>().eq("owner", username).orderByDesc("create_time"));
    }

    @PostMapping("/links")
    public Map<String, Object> addLink(@RequestBody LibraryLink link,
                                       @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        if (link.getUrl() == null || link.getUrl().trim().isEmpty()) {
            res.put("ok", false);
            res.put("message", "网址不能为空");
            return res;
        }
        if (link.getUrl().length() > 500) {
            res.put("ok", false);
            res.put("message", "网址不能超过500个字符");
            return res;
        }

        link.setOwner(username);
        libraryLinkMapper.insert(link);

        res.put("ok", true);
        res.put("data", link);
        return res;
    }

    @DeleteMapping("/links/{id}")
    public Map<String, Object> deleteLink(@PathVariable Long id,
                                          @RequestAttribute(AuthInterceptor.REQ_ATTR_USERNAME) String username) {
        Map<String, Object> res = new HashMap<>();
        libraryLinkMapper.delete(new QueryWrapper<LibraryLink>().eq("id", id).eq("owner", username));
        res.put("ok", true);
        res.put("message", "删除成功");
        return res;
    }
}
