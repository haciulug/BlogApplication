package com.scalefocus.blogapplication.service;

import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.mapper.TagMapper;
import com.scalefocus.blogapplication.model.Tag;
import com.scalefocus.blogapplication.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TagServiceImpl implements TagService {
    private static final Logger LOGGER = LoggerFactory.getLogger(TagServiceImpl.class);
    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    public TagServiceImpl(TagRepository tagRepository, TagMapper tagMapper) {
        this.tagRepository = tagRepository;
        this.tagMapper = tagMapper;
    }

    @Override
    public TagDto createTag(TagDto tagDto) {
        Tag tag = tagMapper.toEntity(tagDto);
        TagDto createdTag = tagMapper.toDto(tagRepository.save(tag));
        LOGGER.info("Tag with id {} created", createdTag.getId());
        return createdTag;
    }

    @Override
    public Tag toEntity(TagDto tagDto) {
        return tagMapper.toEntity(tagDto);
    }

    @Override
    public List<TagDto> getAllTags() {
        return tagRepository.findAll().stream().map(tagMapper::toDto).toList();
    }

    @Override
    public TagDto getTagByName(String name) {
        return tagMapper.toDto(tagRepository.findByName(name).orElse(null));
    }

    @Override
    public List<TagDto> toDtoList(List<Tag> tags) {
        return tags.stream().map(tagMapper::toDto).toList();
    }

    @Override
    public Tag findOrCreateTag(Tag tag) {
        return tagRepository.findByName(tag.getName()).orElseGet(() -> tagRepository.save(tag));
    }
}
