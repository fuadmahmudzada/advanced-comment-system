package com.company.commentsystem.config;

import com.company.commentsystem.model.enums.VoteStatus;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.redisson.api.map.MapWriter;
import org.redisson.api.map.WriteMode;
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
                String sql = "INSERT INTO vote(comment_id, user_id, vote_status) values (?, ?, ?)";
                System.out.println("Write operation has been called for upvote " + map.keySet().toString());

                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    Config config = new Config();
                    config.useSingleServer()
                            .setAddress("redis://127.0.0.1:6379");


                    RedissonClient redissonClient = Redisson.create(config);
                    for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        System.out.println("Votesstatus " + VoteStatus.UP);
                        if(entry.getValue().isEmpty()){
                            continue;
                        }
                        outerloop:
                        for (Long id : entry.getValue()) {
                            for(int i = 0;i<redissonClient.getList("markedForRemoval").size();i++){
                                if(redissonClient.getList("markedForRemoval").get(i).equals(String.valueOf(entry.getKey() + id))){
                                    //redissonClient.getList("markedForRemoval").remove(entry.getKey() + id);
                                    continue outerloop;
                                }
                            }
                            String[] arr = entry.getKey().split(":");
                            preparedStatement.setLong(1, Long.parseLong(arr[arr.length - 1]));
                            preparedStatement.setLong(2, id);
                            preparedStatement.setString(3, String.valueOf(VoteStatus.UP));
                            preparedStatement.addBatch();
                        }



                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    throw new RuntimeException("Write operation failed", e);
                }
            }

            @Override
            public void delete(Collection<String> collection) {
                Config config = new Config();
                config.useSingleServer()
                        .setAddress("redis://127.0.0.1:6379");
                RedissonClient redissonClient = Redisson.create(config);
                String sql = "DELETE FROM vote where id = (select id from vote where comment_id = ? and user_id = ? and vote_status= ?)";
                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    outerloop:
                    for (String entry : collection) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        System.out.println("Votesstatus " + VoteStatus.UP);
                        for(int i = 0;i<redissonClient.getList("markedForRemoval").size();i++){
                            if(redissonClient.getList("markedForRemoval").get(i).equals(String.valueOf(entry))){
                                continue outerloop;
                            }
                        }
                        String[] arr = entry.split(":");
                        preparedStatement.setLong(1, Long.parseLong(arr[arr.length - 2]));
                        preparedStatement.setLong(2, Long.parseLong(arr[arr.length - 1]));
                        preparedStatement.setString(3, String.valueOf(VoteStatus.UP));


                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    throw new RuntimeException("delete operation failed", e);
                }
            }


        };
        return mapWriter;
    }

    @Bean
    public MapWriter<String, Set<Long>> downVoteMapWriter() {
        MapWriter<String, Set<Long>> mapWriter = new MapWriter<String, Set<Long>>() {

            @Override
            public void write(Map<String, Set<Long>> map) {
                String sql = "INSERT INTO vote(comment_id, user_id, vote_status) values (?, ?, ?)";
                System.out.println("Write operation has been called for down vote " + map.keySet().toString());
                Config config = new Config();
                config.useSingleServer()
                        .setAddress("redis://127.0.0.1:6379");


                RedissonClient redissonClient = Redisson.create(config);
                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        String[] keySplit = entry.getKey().split(":");
                        outerloop:
                        for (Long id : entry.getValue()) {
                            for(int i = 0;i<redissonClient.getList("markedForRemoval").size();i++){

                                if(redissonClient.getList("markedForRemoval").get(i).equals(String.valueOf(entry.getKey() + id))){
                                    continue outerloop;
                                }
                            }
                            preparedStatement.setLong(1, Long.parseLong(keySplit[keySplit.length - 1]));
                            preparedStatement.setLong(2, id);
                            preparedStatement.setString(3, String.valueOf(VoteStatus.DOWN));
                            preparedStatement.addBatch();
                        }

                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    throw new RuntimeException("Write operation failed", e);
                }
            }

            @Override
            public void delete(Collection<String> collection) {
                String sql = "DELETE FROM vote where id = (select id from vote where comment_id = ? and user_id = ? and vote_status= ?)";
                Config config = new Config();
                config.useSingleServer()
                        .setAddress("redis://127.0.0.1:6379");
                RedissonClient redissonClient = Redisson.create(config);
                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {
                    outerloop:
                    for (String entry : collection) {
                        for(int i = 0;i<redissonClient.getList("markedForRemoval").size();i++){

                            if(redissonClient.getList("markedForRemoval").get(i).equals(String.valueOf(entry))){
                                continue outerloop;
                            }
                        }
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        System.out.println("Votesstatus " + VoteStatus.UP);

                        String[] arr = entry.split(":");
                        preparedStatement.setLong(1, Long.parseLong(arr[arr.length - 2]));
                        preparedStatement.setLong(2, Long.parseLong(arr[arr.length - 1]));
                        preparedStatement.setString(3, String.valueOf(VoteStatus.DOWN));


                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                } catch (SQLException e) {
                    throw new RuntimeException("delete operation failed", e);
                }



        }


        };
        return mapWriter;

    }

    @Bean
    public MapWriter<String, Set<Long>> downVoteMapOnUpdateWriter() {
        MapWriter<String, Set<Long>> mapWriter = new MapWriter<String, Set<Long>>() {


            @Override
            public void write(Map<String, Set<Long>> map) {
                String sql = "UPDATE vote SET vote_status = ? where id = (select id from vote where vote_status = ? and comment_id = ? and user_id = ? )";
                System.out.println("Write operation has been called for downvote to downVoteMapOnUpdateWriter remove " + map.keySet().toString());
                Config config = new Config();
                config.useSingleServer()
                        .setAddress("redis://127.0.0.1:6379");

                RedissonClient redissonClient = Redisson.create(config);
                System.out.println("VALUES" + map.values());
                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

                    for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
                        System.out.println("Votesstatus " + VoteStatus.UP);
                        outerloop:
                        for (Long id : entry.getValue()) {
                            for(int i = 0;i<redissonClient.getList("markedForRemoval").size();i++){
                                System.out.println("list " + redissonClient.getList("markedForRemoval").get(i));
                                System.out.println(String.valueOf(entry.getValue()).replace("[\\[\\]]", ""));
                                System.out.println("equals "+entry.getKey() + " " + entry.getValue());
                                System.out.println("Get printed "+ String.valueOf(entry.getKey() + entry.getValue()).replaceAll("[\\[\\]]", ""));
                                System.out.println("if contaisn" + redissonClient.getList("markedForRemoval").get(i).equals(entry.getKey() + entry.getValue()));
                                if(redissonClient.getList("markedForRemoval").get(i).equals(String.valueOf(entry.getKey() + ":" + id))){
                                    continue outerloop;
                                }
                            }

//                            if(redissonClient.getList("markedForRemoval").contains(entry.getKey() + entry.getValue())){
//                                continue;
//                            }
                            String[] arr = entry.getKey().split(":");
                            preparedStatement.setLong(3, Long.parseLong(arr[arr.length - 1]));
                            preparedStatement.setLong(4, id);
                            preparedStatement.setString(1, String.valueOf(VoteStatus.DOWN));
                            preparedStatement.setString(2, String.valueOf(VoteStatus.UP));

                            preparedStatement.addBatch();
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
    public MapWriter<String, Set<Long>> upVoteMapOnUpdateWriter() {
        MapWriter<String, Set<Long>> mapWriter = new MapWriter<String, Set<Long>>() {


            @Override
            public void write(Map<String, Set<Long>> map) {
                String sql = "UPDATE vote SET vote_status = ?, comment_id = ? , user_id = ? where id = ?";
                System.out.println("Write operation has been called for downvote to remove " + map.keySet().toString());
                Config config = new Config();
                config.useSingleServer()
                        .setAddress("redis://127.0.0.1:6379");


                RedissonClient redissonClient = Redisson.create(config);
                try (PreparedStatement preparedStatement = conn.prepareStatement(sql)) {

                    for (Map.Entry<String, Set<Long>> entry : map.entrySet()) {
//                        preparedStatement.setLong(1, Long.valueOf(entry.getKey().substring(10)));
                        System.out.println("Votesstatus " + VoteStatus.UP);
                        outerloop:
                        for (Long id : entry.getValue()) {
                            for(int i = 0;i<redissonClient.getList("markedForRemoval").size();i++){
                                if(redissonClient.getList("markedForRemoval").get(i).equals(String.valueOf(entry.getKey() + id))){
                                    continue outerloop;
                                }
                            }
                            String[] arr = entry.getKey().split(":");
                            preparedStatement.setLong(4, Long.parseLong(arr[arr.length - 2]));
                            preparedStatement.setLong(3, id);
                            preparedStatement.setString(1, String.valueOf(VoteStatus.UP));
                            preparedStatement.setLong(2, Long.parseLong(arr[arr.length - 1]));
                            preparedStatement.addBatch();
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
    public RMap<String, Set<Long>> upVoteMap(RedissonClient client, MapWriter<String, Set<Long>> upVoteMapWriter) {
        MapOptions<String, Set<Long>> options = MapOptions.<String, Set<Long>>name("upVoteMap")
                .writer(upVoteMapWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(100000)
                .writeBehindBatchSize(100000);
        return client.getMap(options);
    }


    @Bean
    public RMap<String, Set<Long>> downVoteMap(RedissonClient client, MapWriter<String, Set<Long>> downVoteMapWriter) {
        MapOptions<String, Set<Long>> options = MapOptions.<String, Set<Long>>name("downVoteMap")
                .writer(downVoteMapWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(100000)
                .writeBehindBatchSize(100000);
        return client.getMap(options);
    }


    @Bean
    public RMap<String, Set<Long>> upVoteMapOnUpdate(RedissonClient client, MapWriter<String, Set<Long>> upVoteMapOnUpdateWriter) {
        MapOptions<String, Set<Long>> options = MapOptions.<String, Set<Long>>name("upVoteMapOnUpdate")
                .writer(upVoteMapOnUpdateWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(100000)
                .writeBehindBatchSize(100000);
        return client.getMap(options);
    }

    @Bean
    public RMap<String, Set<Long>> downVoteMapOnUpdate(RedissonClient client, MapWriter<String, Set<Long>> downVoteMapOnUpdateWriter) {
        MapOptions<String, Set<Long>> options = MapOptions.<String, Set<Long>>name("downVoteMapOnUpdate")
                .writer(downVoteMapOnUpdateWriter)
                .writeMode(WriteMode.WRITE_BEHIND)
                .writeBehindDelay(100000)
                .writeBehindBatchSize(100000);
        return client.getMap(options);
    }



    @Bean
    public RSet<String> list(RedissonClient client) {
        RSet<String> set = client.getSet("markedForRemove");
        return set;
    }
}
