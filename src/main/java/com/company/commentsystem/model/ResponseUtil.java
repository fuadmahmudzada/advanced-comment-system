package com.company.commentsystem.model;

import com.company.commentsystem.model.dto.response.ApiResponse;

public class ResponseUtil {
    public static <E> ApiResponse<E> success(String message, E data, Object metaData){
        return new ApiResponse<>(message, "success", data, metaData);
    }

    public static <E>  ApiResponse<E> fail(E data, String message, Object metaData){
        return new ApiResponse<>(message, "fail", data, metaData);
    }
}

