package com.company.commentsystem.service.impl;

import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.dao.repository.MeetingRepository;
import com.company.commentsystem.model.dto.meeting.MeetingCreateDto;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;
import com.company.commentsystem.model.mapper.MeetingMapper;
import com.company.commentsystem.service.MeetingService;
import com.company.commentsystem.utils.SuffixGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MeetingServiceImpl implements MeetingService {
    private final MeetingRepository meetingRepository;
    private final SuffixGenerator suffixGenerator = new SuffixGenerator();

    @Transactional
    public MeetingResponseDto create(MeetingCreateDto meetingCreateDto){
        Meeting meeting = new Meeting();
        meeting.setLink(meetingCreateDto.getLink());
        meeting.setPlatformLink(suffixGenerator.generatePlatformLink());
        return MeetingMapper.INSTANCE.toMeetingResponseDto(meetingRepository.save(meeting));

    }
}
