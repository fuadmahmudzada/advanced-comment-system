package com.company.commentsystem;

import gears.ExecutionMode;
import gears.GearsBuilder;
import gears.operations.OnRegisteredOperation;
import gears.readers.KeysReader;
import gears.records.KeysReaderRecord;

public class RedisGears {
    public static void main(String[] args) {
        KeysReader keysReader = new KeysReader();

        GearsBuilder<KeysReaderRecord> gearsBuilder = GearsBuilder.CreateGearsBuilder(keysReader);

        gearsBuilder.filter((r)->r.getKey().startsWith("commentapp:comment:key:"));


        gearsBuilder.foreach(record -> {
           record
        });
        gearsBuilder.register(ExecutionMode.ASYNC, )
    }
}
