package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.model.Tag;

import java.util.List;

public interface TagService {
    TagDto createTag(TagDto name);
    List<TagDto> getAllTags();
    TagDto getTagByName(String name);

    List<TagDto> toDtoList(List<Tag> tags);

    Tag toEntity(TagDto tagDto);
}
