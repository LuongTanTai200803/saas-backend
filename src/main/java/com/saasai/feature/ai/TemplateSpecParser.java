package com.saasai.feature.ai;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * TemplateSpecParser is responsible for parsing template specifications from text.
 * It extracts metadata and section specifications from the provided template text.
 */
@Component
public class TemplateSpecParser {
        private static final Pattern TEN_LOAI_PATTERN =
            Pattern.compile(
                    "(?:tenLoaiVanBan|ten_loai_van_ban)\\s*:\\s*\"([^\"]*)\"",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern CAU_DAN_PATTERN =
            Pattern.compile(
                    "(?:cauDanChuyenTiep|cau_dan_chuyen_tiep)\\s*:\\s*\"([\\s\\S]*?)\"",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern CAU_KET_PATTERN =
            Pattern.compile(
                    "(?:cauKetHanhChinh|cau_ket_hanh_chinh)\\s*:\\s*\"([\\s\\S]*?)\"",
                    Pattern.CASE_INSENSITIVE
            );

    private static final Pattern SECTION_BLOCK_PATTERN =
        Pattern.compile(
                "Mục\\s*\\d+\\s*:\\s*[\\s\\S]*?"
                + "\\+\\s*(?:tieuDeMuc|tieu_de_muc)\\s*:\\s*\"([^\"]+)\""
                + "[\\s\\S]*?"
                + "\\+\\s*(?:noiDungChiTiet|noi_dung_chi_tiet)\\s*:\\s*([\\s\\S]*?)(?=\\n\\s*-?\\s*Mục\\s*\\d+\\s*:|\\z)",
                Pattern.CASE_INSENSITIVE
        );

    public TemplateDocument.TemplateMeta parseMeta(String templateText) {
        TemplateDocument.TemplateMeta meta = new TemplateDocument.TemplateMeta();

        if (!StringUtils.hasText(templateText)) {
            meta.setTenLoaiVanBan("NGHỊ QUYẾT");
            meta.setCauDanChuyenTiep("");
            meta.setCauKetHanhChinh("");
            return meta;
        }

        meta.setTenLoaiVanBan(extractQuoted(templateText, TEN_LOAI_PATTERN, "NGHỊ QUYẾT"));
        meta.setCauDanChuyenTiep(unescape(extractQuoted(templateText, CAU_DAN_PATTERN, "")));
        meta.setCauKetHanhChinh(unescape(extractQuoted(templateText, CAU_KET_PATTERN, "")));
        return meta;
    }

    public List<TemplateDocument.SectionSpec> parseSectionSpecs(String templateText) {
        List<TemplateDocument.SectionSpec> specs = new ArrayList<>();

        if (!StringUtils.hasText(templateText)) {
            return specs;
        }

        Matcher matcher = SECTION_BLOCK_PATTERN.matcher(templateText);
        while (matcher.find()) {
            TemplateDocument.SectionSpec spec = new TemplateDocument.SectionSpec();
            spec.setTieuDeMacDinh(safe(matcher.group(1)));
            spec.setGoiYNoiDung(cleanGuide(safe(matcher.group(2))));
            specs.add(spec);
        }

        return specs;
    }

    private String extractQuoted(String text, Pattern pattern, String defaultValue) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return safe(m.group(1));
        }
        return defaultValue;
    }

    private String cleanGuide(String value) {
        String v = value.replaceFirst("^\\s*Nêu rõ\\s*", "Nêu rõ ");
        return v.trim();
    }

    private String unescape(String value) {
        return value.replace("\\n", "\n").trim();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
