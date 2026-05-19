package com.zhangwenzheng.smartschedule.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhangwenzheng.smartschedule.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
}

