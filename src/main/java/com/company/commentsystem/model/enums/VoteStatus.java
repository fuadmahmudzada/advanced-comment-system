package com.company.commentsystem.model.enums;


import jakarta.validation.constraints.NotNull;
import lombok.*;


public enum VoteStatus {
    DOWN,
    UP,
    NEUTRAL;


    public static VoteStatus getByOrdinal(Integer ordinal){
        return VoteStatus.values()[ordinal];
    }
}
