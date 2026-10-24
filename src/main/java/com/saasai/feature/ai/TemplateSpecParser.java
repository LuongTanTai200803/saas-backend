package com.saasai.feature.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TemplateSpecParser is responsible for parsing template specifications from
 * text.
 * It extracts metadata and section specifications from the provided template
 * text.
 */
@Component
public class TemplateSpecParser {

    private final ObjectMapper objectMapper;

    public TemplateSpecParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<TemplateDocument.SectionSpec> parseSectionSpecs(
            String templateDefinitionJson) {

        List<TemplateDocument.SectionSpec> specs = new ArrayList<>();

        if (!StringUtils.hasText(templateDefinitionJson)) {
            return specs;
        }

        try {
            JsonNode root = objectMapper.readTree(templateDefinitionJson);
            JsonNode cacMuc = root.path("cacMuc");

            if (!cacMuc.isArray()) {
                return specs;
            }

            for (JsonNode muc : cacMuc) {
                TemplateDocument.SectionSpec spec = new TemplateDocument.SectionSpec();

                spec.setCode(text(muc, "code", ""));
                spec.setTieuDeMacDinh(
                        text(muc, "tieuDeMuc", ""));
                spec.setGoiYNoiDung(
                        text(muc, "noiDungChiTiet", ""));

                specs.add(spec);
            }

            return specs;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "template_definition_json không phải JSON hợp lệ",
                    exception);
        }
    }

    private String text(
            JsonNode node,
            String field,
            String defaultValue) {
        JsonNode value = node.get(field);

        return value == null || value.isNull()
                ? defaultValue
                : value.asText(defaultValue);
    }

    public TemplateDocument.TemplateMeta parseMeta(String templateDefinitionJson) {
        TemplateDocument.TemplateMeta meta = new TemplateDocument.TemplateMeta();

        meta.setTenLoaiVanBan("NGHỊ QUYẾT");
        meta.setCauDanChuyenTiep("");
        meta.setCauKetHanhChinh("");

        if (!StringUtils.hasText(templateDefinitionJson)) {
            return meta;
        }

        try {
            JsonNode root = objectMapper.readTree(templateDefinitionJson);

            meta.setTenLoaiVanBan(
                    text(root, "tenLoaiVanBan", "NGHỊ QUYẾT"));
            meta.setCauDanChuyenTiep(
                    text(root, "cauDanChuyenTiep", ""));
            meta.setCauKetHanhChinh(
                    text(root, "cauKetHanhChinh", ""));

            return meta;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "template_definition_json không phải JSON hợp lệ",
                    exception);
        }
    }
}