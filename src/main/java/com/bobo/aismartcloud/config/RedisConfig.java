package com.bobo.aismartcloud.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 聊天记忆用 Redis 客户端（条件化）。
 *
 * 只有当 chat.memory.redis.enabled=true 时才注册 bean，
 * 默认 matchIfMissing=false —— 用户没显式开启时整个配置类不加载。
 *
 * 即使开启了，Redis 暂时连不上也不会让应用启动失败：
 * 实际连接是 lazy 的（StringRedisTemplate.opsForValue() 才触发），
 * 加上 TwoTierChatMemoryRepository 内部有 try/catch 兜底，应用会降级到 PG-only 模式。
 */
@Configuration
@ConditionalOnClass(StringRedisTemplate.class)
@ConditionalOnProperty(name = "chat.memory.redis.enabled", havingValue = "true", matchIfMissing = false)
public class RedisConfig {

    @Bean
    public StringRedisTemplate chatMemoryStringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
