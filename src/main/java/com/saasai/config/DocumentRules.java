package com.saasai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Data
@Component
@Configuration
@PropertySource("classpath:document-style.properties")
@ConfigurationProperties(prefix = "document")
public class DocumentRules {

    public FontRule font;
    public PageRule page;
    public BodyRule body;
    public TextRule heading;
    public TextRule title;
    public TextRule sectionHeading;
    
    public LayoutRule header;
    public LayoutRule footer;
    public LayoutRule signature;
    public LayoutRule recipient;

    public RecipientSignatureRule recipientSignature = new RecipientSignatureRule(); ;
   
    @Data
    public static class RecipientSignatureRule {

        /**
         * Phần Nơi nhận chiếm bao nhiêu %
         * chiều rộng bảng.
         */
        public double leftWidth;

        /**
         * Phần T/M ... ký tên chiếm bao nhiêu %
         * chiều rộng bảng.
         */
        public double rightWidth;
    }


    public static class FontRule {
        public String family;
    }

    public static class PageRule {
        public MarginRule margin;
    }

    public static class MarginRule {
        public double leftMm;
        public double rightMm;
        public double topMm;
        public double bottomMm;
    }

    public static class BodyRule {
        public double fontSize;
        public double lineHeight;
        public double paragraphSpacingPt;
        public String alignment;
    }

    public static class TextRule {
        public double fontSize;
        public boolean bold;
        public String alignment;
    }

    public static class LayoutRule {
        public boolean enabled;
        public String layout;
        public boolean pageNumber;

        public double leftWidth;
        public double rightWidth;
    }
}
