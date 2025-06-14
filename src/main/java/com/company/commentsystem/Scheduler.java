package com.company.commentsystem;

import com.company.commentsystem.dao.entity.Comment;
import com.company.commentsystem.dao.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Configuration
@EnableScheduling
public class Scheduler {

    private final CommentRepository commentRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private int count;
    @Scheduled(fixedRate = 10000)
    public void addToDb(){
        System.out.println("Scheduler start running");
        Set<String> keys = redisTemplate.keys("*commentapp:comment:key:*");
        System.out.println("Keys " + keys.toString());
        List<String> commentIdList = new ArrayList<>();
        List<Comment> commentList =new ArrayList<>();
        for(String key : keys){
            String commentId = key.split(":")[3];
            commentIdList.add(commentId);
        }
        System.out.println("Comment id List " + commentIdList);
        for(String id : commentIdList){
            redisTemplate.opsForHash();
            Map<Object, Object> commentMap = redisTemplate.opsForHash().entries("commentapp:comment:key:" + id);
            commentList.add(new Comment(commentMap));
        }
        System.out.println("comment list: " + commentList.toString());
        commentRepository.saveAll(commentList);
        count++;
        System.out.println("count " + count );
        if(count==3) {
//            List<String> keys = new ArrayList<>()
//            redisTemplate.delete()
            Boolean isDeleted = redisTemplate.delete("commentapp:comment:key:");
            System.out.println(isDeleted);
            count =0;
        }

    }

}
