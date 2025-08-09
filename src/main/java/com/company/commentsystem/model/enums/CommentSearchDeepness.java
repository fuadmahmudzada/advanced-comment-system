package com.company.commentsystem.model.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum CommentSearchDeepness {
    SUBCOMMENTS_DEPTH_1(0), SUBCOMMENTS_DEPTH_2(1), SUBCOMMENTS_DEPTH_3(2), SUBCOMMENTS_ALL(3);
    private static final Map<Integer, CommentSearchDeepness> lookup = new HashMap<>();
    static {
        for(CommentSearchDeepness commentSearchDeepness : EnumSet.allOf(CommentSearchDeepness.class))
            lookup.put(commentSearchDeepness.ord, commentSearchDeepness);
    }

    private int ord;

    private CommentSearchDeepness(int ord){
        this.ord = ord;
    }

    public int getOrd(){
        return this.ord;
    }
    public static CommentSearchDeepness getInstance(int ord){
        return lookup.get(ord);
    }
}
