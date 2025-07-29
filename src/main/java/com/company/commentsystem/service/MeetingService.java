package com.company.commentsystem.service;

import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.dao.repository.MeetingRepository;
import com.company.commentsystem.model.dto.meeting.MeetingCreateDto;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;
import com.company.commentsystem.utils.SuffixGenerator;
import org.springframework.stereotype.Service;

@Service
public class MeetingService {
    private final MeetingRepository meetingRepository;
    private final SuffixGenerator suffixGenerator = new SuffixGenerator();
    public MeetingService(MeetingRepository meetingRepository) {
        this.meetingRepository = meetingRepository;
    }

    public MeetingResponseDto create(MeetingCreateDto meetingCreateDto){
        Meeting meeting = new Meeting();
        meeting.setLink(meetingCreateDto.getLink());
        meeting.setPlatformLink(suffixGenerator.generatePlatformLink());
        Meeting newMeeting = meetingRepository.save(meeting);
        MeetingResponseDto meetingResponseDto = new MeetingResponseDto();
        meetingResponseDto.setId(newMeeting.getId());
        meetingResponseDto.setLink(newMeeting.getLink());
        meetingResponseDto.setPlatformLink(meeting.getPlatformLink());
        return meetingResponseDto;

    }
}
