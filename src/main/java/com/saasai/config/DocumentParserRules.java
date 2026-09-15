package com.saasai.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Configuration
@PropertySource(
    value = "classpath:document-layout-rules.properties",
    encoding = "UTF-8"
)
@ConfigurationProperties(prefix = "document")
@Component
@Data
public class DocumentParserRules {

    public String headerLeftPattern;
    public String headerRightPattern;

    public String headerDateLeftPattern;
    public String headerDateRightPattern;

    public String recipientPattern;
    public String signaturePattern;

    public String sectionPattern;
    public String subSectionPattern;

    public String titlePattern;
    public String headingPattern;
    public String titleSubPattern;
}