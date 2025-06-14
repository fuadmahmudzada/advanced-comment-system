package com.company.commentsystem.controller;

import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.model.dto.meeting.MeetingCreateDto;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;
import com.company.commentsystem.service.MeetingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
public class MeetingController {
    private final MeetingService meetingService;

    public MeetingController(MeetingService meetingService) {
        this.meetingService = meetingService;
    }

    @PostMapping
    public ResponseEntity<MeetingResponseDto> create(@RequestBody MeetingCreateDto meetingCreateDto) throws URISyntaxException {
        MeetingResponseDto meetingResponseDto = meetingService.create(meetingCreateDto.getLink());
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingResponseDto);
    }
}
