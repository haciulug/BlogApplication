package com.scalefocus.blogapplication.mapper;

import com.scalefocus.blogapplication.dto.MediaFileDto;
import com.scalefocus.blogapplication.model.MediaFile;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MediaFileMapper {
    MediaFile toEntity(MediaFileDto mediaFileDto);
    MediaFileDto toDto(MediaFile mediaFile);
}
