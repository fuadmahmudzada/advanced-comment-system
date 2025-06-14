package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.dao.repository.MeetingRepository;
import com.company.commentsystem.model.dto.meeting.MeetingCreateDto;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;
import org.springframework.stereotype.Service;

@Service
public class MeetingService {
    private final MeetingRepository meetingRepository;

    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public MeetingResponseDto create(String link){
        Meeting meeting = new Meeting();
        meeting.setLink(link);
        Meeting newMeeting = meetingRepository.save(meeting);
        MeetingResponseDto meetingResponseDto = new MeetingResponseDto();
        meetingResponseDto.setId(newMeeting.getId());
        meetingResponseDto.setLink(newMeeting.getLink());
        return meetingResponseDto;

    }
}
