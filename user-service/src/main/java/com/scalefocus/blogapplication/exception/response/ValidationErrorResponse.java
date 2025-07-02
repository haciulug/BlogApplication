package com.scalefocus.blogapplication.exception.response;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@Getter
@SuperBuilder
public class ValidationErrorResponse extends BaseErrorResponse {
    private final Map<String, List<String>> errors;

}