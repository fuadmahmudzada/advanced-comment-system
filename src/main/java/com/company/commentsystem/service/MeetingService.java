package com.company.commentsystem.service;

import com.company.commentsystem.model.dto.meeting.MeetingCreateDto;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;

public interface MeetingService {
    MeetingResponseDto create(MeetingCreateDto meetingCreateDto);
}
