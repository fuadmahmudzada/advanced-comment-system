//package com.company.commentsystem.controller;
//
//import com.company.commentsystem.model.dto.ExceptionResponse;
//import org.springframework.http.HttpStatus;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.ResponseStatus;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//import org.postgresql.util.PSQLException;
//
//import java.sql.SQLException;
//
//@RestControllerAdvice
//public class ControllerAdvice {
//    @ExceptionHandler(SQLException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public ExceptionResponse handle(PSQLException e) {
//        return new ExceptionResponse(e.getMessage());
//    }
//
//    @ExceptionHandler(RuntimeException.class)
//    @ResponseStatus(HttpStatus.BAD_REQUEST)
//    public ExceptionResponse handle(RuntimeException e) {
//        return new ExceptionResponse(e.getMessage());
//    }
//}
