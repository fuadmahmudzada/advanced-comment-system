package com.company.commentsystem;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.model.dto.CommentDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
public class RedisConfig {

    @Bean
    public JedisConnectionFactory jedisConnectionFactory()
    {

        // redis server properties we write here if we are in same machine than there is no need to write properties

        // jedisConnectionFactory.setHostName("localhost");
        // jedisConnectionFactory.setPort(6379);

        return new JedisConnectionFactory();
    }

    @Bean
    public RedisTemplate<String, CommentDto> redisTemplate()
    {
        RedisTemplate<String, CommentDto> redisTemplate=new RedisTemplate<>();
        redisTemplate.setConnectionFactory(jedisConnectionFactory());
        return redisTemplate;
    }
}