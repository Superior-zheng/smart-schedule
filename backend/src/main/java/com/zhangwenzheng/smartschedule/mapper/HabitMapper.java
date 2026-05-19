package com.zhangwenzheng.smartschedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangwenzheng.smartschedule.entity.Habit;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HabitMapper extends BaseMapper<Habit> {
}
