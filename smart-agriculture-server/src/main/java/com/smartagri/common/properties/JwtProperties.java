package com.smartagri.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "smart-agri.jwt")
public class JwtProperties {

    private String secretKey;
    private Integer expireHours = 12;
    private String issuer;
    private String tokenPrefix = "Bearer";
    private String headerName = "Authorization";
    private List<String> ignoreUrls = new ArrayList<>();
}
