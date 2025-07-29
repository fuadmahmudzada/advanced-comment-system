package com.company.commentsystem.utils;

import org.springframework.batch.item.xml.stax.UnclosedElementCollectingEventWriter;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public class SuffixGenerator {
    private final int BOTTOM_LIMIT = 32;
    private final int UPPER_LIMIT = 123;
    private final char[] LETTERS = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};
    private final int PLATFORM_LINK_LENGTH = 30;
    public String generateName() {
        RandomGenerator randomGenerator = new Random();
        String randomSuffix = randomGenerator.ints(15, BOTTOM_LIMIT, UPPER_LIMIT).mapToObj(x-> ((char)x) + "").collect(Collectors.joining());
        System.out.println("random " + randomSuffix);
        return randomSuffix;
    }

    public String generatePlatformLink(){
        char[] chars = new char[PLATFORM_LINK_LENGTH];
        Random random = new Random();
        for(int i = 0; i<chars.length; i++){
            chars[i] = LETTERS[(char) random.nextInt(0, LETTERS.length)];
        }
        System.out.println();
        return new String(chars);
    }
}
