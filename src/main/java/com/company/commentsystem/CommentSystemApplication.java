package com.company.commentsystem;

import jakarta.persistence.Cacheable;
import org.springframework.batch.item.redis.RedisItemReader;
import org.springframework.batch.item.redis.builder.RedisItemReaderBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class CommentSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommentSystemApplication.class, args);
    }

//    ScanOptions scanOptions = ScanOptions.scanOptions()
//            .match()
//
//    protected RedisItemReader<String, String> reader(){
//        return new RedisItemReaderBuilder<String, String>()
//                .scanOptions()
//    }

}
