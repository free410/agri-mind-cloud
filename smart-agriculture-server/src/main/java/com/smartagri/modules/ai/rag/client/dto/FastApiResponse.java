package com.smartagri.modules.ai.rag.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastApiResponse<T> {

    private Integer code;

    private String message;

    private T data;

    private String timestamp;
}
