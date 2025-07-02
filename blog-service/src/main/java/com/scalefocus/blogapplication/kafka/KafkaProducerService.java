package com.scalefocus.blogapplication.kafka;

import com.scalefocus.blogapplication.dto.BlogEventDto;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, BlogEventDto> kafkaTemplate;

    public void sendBlogEvent(BlogEventDto blogEventDto) {
        kafkaTemplate.send("blog-events", blogEventDto.getBlogId().toString(), blogEventDto);
    }
}
