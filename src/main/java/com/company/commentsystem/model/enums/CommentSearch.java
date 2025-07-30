package com.company.commentsystem.model.enums;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public enum CommentSearch {
    SUBCOMMENTS_DEPTH_1(0), SUBCOMMENTS_DEPTH_2(1), SUBCOMMENTS_DEPTH_3(2), SUBCOMMENTS_ALL(3);
    private static final Map<Integer, CommentSearch> lookup = new HashMap<>();
    static {
        for(CommentSearch commentSearch : EnumSet.allOf(CommentSearch.class))
            lookup.put(commentSearch.ord, commentSearch);
    }

    private int ord;

    private CommentSearch(int ord){
        this.ord = ord;
    }

    public int getOrd(){
        return this.ord;
    }
    public static CommentSearch getInstance(int ord){
        return lookup.get(ord);
    }
}
