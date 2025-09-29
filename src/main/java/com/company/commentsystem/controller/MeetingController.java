package com.company.commentsystem.controller;

import com.company.commentsystem.model.ResponseUtil;
import com.company.commentsystem.model.dto.response.ApiResponse;
import com.company.commentsystem.model.dto.meeting.MeetingCreateDto;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;
import com.company.commentsystem.service.impl.MeetingServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("v1/meeting")
@RequiredArgsConstructor
public class MeetingController {
    private final MeetingServiceImpl meetingServiceImpl;

    @PostMapping
    public ResponseEntity<ApiResponse<MeetingResponseDto>> create(@RequestBody MeetingCreateDto meetingCreateDto) {
        MeetingResponseDto meetingResponseDto = meetingServiceImpl.create(meetingCreateDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseUtil.success( "Meeting created", meetingResponseDto, null)
        );
    }
}
