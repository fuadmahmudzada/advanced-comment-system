package com.company.commentsystem;

import org.redisson.Redisson;
import org.redisson.api.RLocalCachedMap;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.MapWriter;
import org.redisson.api.map.WriteMode;
import org.redisson.api.options.LocalCachedMapOptions;
import org.redisson.api.options.MapOptions;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

@Configuration
public class RedissonConfig {
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://127.0.0.1:6379");

        return Redisson.create(config);
    }
    Connection conn;

    {
        try {
            conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/comment_system", "postgres", "10062005");
        } catch (SQLException e) {
            throw new RuntimeException("Database connection failed", e);
        }
    }

    @Bean
    public MapWriter<String, Long> upVoteMapWriter() {
        MapWriter<String, Long> mapWriter = new MapWriter<String, Long>() {


            @Override
            public void write(Map<String, Long> map) {
                String sql = "UPDATE comment SET up_vote = ? where id = ?";
                System.out.println("Write operation has been called for upvote");

                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Long> entry : map.entrySet()) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        preparedStatement.setLong(1, entry.getValue());
                        preparedStatement.setLong(2, Long.parseLong(entry.getKey().substring(entry.getKey().length()-1)));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    throw new RuntimeException("Write operation failed", e);
                }
            }

            @Override
            public void delete(Collection<String> collection) {

            }


        };
        return mapWriter;
    }

    @Bean
    public MapWriter<String, Long> downVoteMapWriter(){
        MapWriter<String, Long> mapWriter = new MapWriter<String, Long>() {

            @Override
            public void write(Map<String, Long> map) {
                String sql = "UPDATE comment SET down_vote = ? WHERE id = ?";
                System.out.println("Write operation has been called for down vote");

                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Long> entry : map.entrySet()) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        preparedStatement.setLong(1, entry.getValue());
                        preparedStatement.setLong(2, Long.parseLong(entry.getKey().substring(entry.getKey().length()-1)));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    throw new RuntimeException("Write operation failed", e);
                }
            }

            @Override
            public void delete(Collection<String> collection) {

            }


        };
        return mapWriter;

    }

    @Bean
    public RLocalCachedMap<String, Long> upVoteMap(RedissonClient client, MapWriter<String, Long> upVoteMapWriter) {
        LocalCachedMapOptions<String, Long> options = LocalCachedMapOptions.<String, Long>name("upVoteMap")
                .writer(upVoteMapWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(20000)
                .writeBehindBatchSize(100000);
        return client.getLocalCachedMap(options);
    }


    @Bean
    public RLocalCachedMap<String, Long> downVoteMap(RedissonClient client, MapWriter<String, Long> downVoteMapWriter) {
        LocalCachedMapOptions<String, Long> options = LocalCachedMapOptions.<String, Long>name("downVoteMap")
                .writer(downVoteMapWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(20000)
                .writeBehindBatchSize(100000);
        return client.getLocalCachedMap(options);
    }
}
