package com.company.commentsystem.controller;

import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.model.dto.meeting.MeetingCreateDto;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;
import com.company.commentsystem.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URISyntaxException;

@RestController
@RequestMapping("v1/meeting")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingService meetingService;

    @PostMapping
    public ResponseEntity<MeetingResponseDto> create(@RequestBody MeetingCreateDto meetingCreateDto) throws URISyntaxException {
        MeetingResponseDto meetingResponseDto = meetingService.create(meetingCreateDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(meetingResponseDto);
    }
}
