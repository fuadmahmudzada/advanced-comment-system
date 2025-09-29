package com.company.commentsystem.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class ApiResponse<T> {
    private String message;
    private String status;
    private T data;
    private Object metaData;
}
