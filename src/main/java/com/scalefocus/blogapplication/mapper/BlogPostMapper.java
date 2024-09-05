package com.scalefocus.blogapplication.mapper;

import com.scalefocus.blogapplication.dto.BlogPostDto;
import com.scalefocus.blogapplication.dto.BlogPostSummaryDto;
import com.scalefocus.blogapplication.model.BlogPost;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring")
public interface BlogPostMapper {

    int SUMMARY_LENGTH = 20;
    BlogPostMapper INSTANCE = Mappers.getMapper(BlogPostMapper.class);


    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "title", source = "title"),
            @Mapping(target = "summary", source = "content", qualifiedByName = "summary"),
    })
    BlogPostSummaryDto toSummaryDto(BlogPost blogPost);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "title", source = "title"),
            @Mapping(target = "content", source = "content"),
            @Mapping(target = "tags", source = "tags")
    })
    BlogPostDto toDto(BlogPost blogPost);

    @Mappings({
            @Mapping(target = "id", source = "id"),
            @Mapping(target = "title", source = "title"),
            @Mapping(target = "content", source = "content"),
            @Mapping(target = "tags", source = "tags")
    })
    BlogPost toEntity(BlogPostDto blogPostDto);

    List<BlogPostDto> toDtoList(List<BlogPost> blogPosts);

    @Named("summary")
    default String toSummary(String content) {
        return content.substring(0, Math.min(content.length(), SUMMARY_LENGTH));
    }

}
