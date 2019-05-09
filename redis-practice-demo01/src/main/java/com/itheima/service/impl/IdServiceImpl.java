package com.itheima.service.impl;

import com.itheima.domain.LogInfo;
import com.itheima.mapper.IdMapper;
import com.itheima.service.IdService;
import org.springframework.data.redis.core.RedisTemplate;
import java.util.concurrent.TimeUnit;

public class IdServiceImpl implements IdService {

    private IdMapper idMapper;

    private RedisTemplate redisTemplate;

    @Override
    public LogInfo findById(String id) {
        LogInfo logInfo = (LogInfo) redisTemplate.opsForValue().get(id);

        //双重检查锁防止redis被击穿
        if (logInfo == null) {
            synchronized (this) {
                logInfo = (LogInfo) redisTemplate.opsForValue().get(id);

                if (logInfo == null) {

                    //当缓存为空时，查询数据库
                    logInfo = (LogInfo) idMapper.selectByPrimaryKey(Integer.valueOf(id));

                    if (logInfo == null) {
                        //说明id在数据库中不存在，做单独处理
                        redisTemplate.opsForValue().set(id, logInfo, 60, TimeUnit.SECONDS);
                    }
                    //设置可用数据在redis中存放10天
                    redisTemplate.opsForValue().set(id, logInfo, 10, TimeUnit.DAYS);
                }
            }
        }
        return logInfo;
    }
}

