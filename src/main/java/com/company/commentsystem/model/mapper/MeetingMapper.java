package com.company.commentsystem.model.mapper;

import com.company.commentsystem.dao.entity.Meeting;
import com.company.commentsystem.model.dto.meeting.MeetingResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;


@Mapper
public abstract class MeetingMapper {
    public static final MeetingMapper INSTANCE = Mappers.getMapper(MeetingMapper.class);

    public abstract MeetingResponseDto toMeetingResponseDto(Meeting meeting);
}
