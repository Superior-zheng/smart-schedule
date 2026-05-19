package com.zhangwenzheng.smartschedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangwenzheng.smartschedule.entity.Task;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaskMapper extends BaseMapper<Task> {
    // 继承 BaseMapper 后，拥有了 insert, delete, update, select 等所有方法
}
