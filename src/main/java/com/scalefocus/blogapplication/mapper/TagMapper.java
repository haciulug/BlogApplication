package com.scalefocus.blogapplication.mapper;

import com.scalefocus.blogapplication.dto.TagDto;
import com.scalefocus.blogapplication.model.Tag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper(componentModel = "spring")
public interface TagMapper {


    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "name", source = "name")
    })
    TagDto toDto(Tag tag);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "name", source = "name")
    })
    Tag toEntity(TagDto tagDto);
}
