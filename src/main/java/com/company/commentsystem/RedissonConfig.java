package com.company.commentsystem;

import com.company.commentsystem.model.enums.VoteStatus;
import org.postgresql.util.PSQLException;
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
import java.util.Set;

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
    public MapWriter<String, Set<Long>> upVoteMapWriter() {
        MapWriter<String, Set<Long>> mapWriter = new MapWriter<String, Set<Long>>() {


            @Override
            public void write(Map<String, Set<Long>> map) {
                String sql = "INSERT INTO vote(comment_id, users_id, vote_status) values (?, ?, ?)";
                System.out.println("Write operation has been called for upvote " + map.keySet().toString());

                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        System.out.println("Votesstatus " + VoteStatus.UP);
                        for(Long id : entry.getValue()){
                            String[] arr = entry.getKey().split(":");
                            preparedStatement.setLong(1, Long.parseLong(arr[arr.length-1]));
                            preparedStatement.setLong(2, id);
                            preparedStatement.setString(3, String.valueOf(VoteStatus.UP));
                        }


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
    public MapWriter<String, Set<Long>> downVoteMapWriter(){
        MapWriter<String, Set<Long>> mapWriter = new MapWriter<String, Set<Long>>() {

            @Override
            public void write(Map<String, Set<Long>> map) {
                String sql = "INSERT INTO vote(comment_id, users_id, vote_status) values (?, ?, ?)";
                System.out.println("Write operation has been called for down vote " + map.keySet().toString());

                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        for(Long id : entry.getValue()){
                            preparedStatement.setLong(1, Long.parseLong(entry.getKey().substring(entry.getKey().length()-1)));
                            preparedStatement.setLong(2, id);
                            preparedStatement.setString(3, VoteStatus.DOWN.toString());
                        }
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
    public RLocalCachedMap<String, Set<Long>> upVoteMap(RedissonClient client, MapWriter<String, Set<Long>> upVoteMapWriter) {
        LocalCachedMapOptions<String, Set<Long>> options = LocalCachedMapOptions.<String, Set<Long>>name("upVoteMap")
                .writer(upVoteMapWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(30000)
                .writeBehindBatchSize(100000);
        return client.getLocalCachedMap(options);
    }


    @Bean
    public RLocalCachedMap<String, Set<Long>> downVoteMap(RedissonClient client, MapWriter<String, Set<Long>> downVoteMapWriter) {
        LocalCachedMapOptions<String, Set<Long>> options = LocalCachedMapOptions.<String, Set<Long>>name("downVoteMap")
                .writer(downVoteMapWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(30000)
                .writeBehindBatchSize(100000);
        return client.getLocalCachedMap(options);
    }
}
