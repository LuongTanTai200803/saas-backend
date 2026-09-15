package com.saasai.feature.ai;

import com.deepoove.poi.XWPFTemplate;
import com.deepoove.poi.config.Configure;
import com.deepoove.poi.data.ParagraphRenderData;
import com.deepoove.poi.data.TextRenderData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.dto.DocumentFormDTO;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class NghiQuyetExportService {

        private final ObjectMapper objectMapper;
        private final TemplateSpecParser templateSpecParser;

        public NghiQuyetExportService(ObjectMapper objectMapper, TemplateSpecParser templateSpecParser) {
                this.objectMapper = objectMapper;
                this.templateSpecParser = templateSpecParser;
        }

        public byte[] renderNghiQuyetDocx(
                DocumentFormDTO form,
                String topicCode,
                String templateCode,
                String templateContent,
                TemplateDocument.Body body
        ) throws Exception {

        TemplateDocument.TemplateMeta meta =
                templateSpecParser.parseMeta(templateContent);

        List<TemplateDocument.SectionSpec> specs =
                templateSpecParser.parseSectionSpecs(templateContent);

        System.out.println("=== TEMPLATE SPEC PARSE ===");
        System.out.println("topicCode = " + topicCode);
        System.out.println("templateCode = " + templateCode);
        System.out.println("tenLoaiVanBan = " + meta.getTenLoaiVanBan());
        System.out.println("cauDanChuyenTiep = " + meta.getCauDanChuyenTiep());
        System.out.println("cauKetHanhChinh = " + meta.getCauKetHanhChinh());
        System.out.println("specs size = " + specs.size());

        for (int i = 0; i < specs.size(); i++) {
                System.out.println(
                        "spec[" + i + "].title = "
                                + specs.get(i).getTieuDeMacDinh()
                );
        }

        System.out.println("==========================");

        TemplateDocument.Body alignedBody =
                alignBodyToSpec(body, specs);

        Map<String, Object> data = new HashMap<>();

        data.putAll(buildFixedDocxFields(form, meta));

        data.put(
                "doan_can_cu",
                nullToEmpty(alignedBody.getDoanCanCu())
        );

        // =========================
        // CAC MUC
        // =========================

        List<Map<String, Object>> cacMucRender = new ArrayList<>();

        for (TemplateDocument.BodySection section : alignedBody.getCacMuc()) {

                Map<String, Object> item = new HashMap<>();

                // Tiêu đề vẫn là String bình thường
                item.put(
                        "tieuDeMuc",
                        nullToEmpty(section.getTieuDeMuc())
                );

                // Nội dung GIỮ NGUYÊN String.
                // Không split ở đây.
                String content = section.getNoiDungChiTiet();

                if (content == null || content.isBlank()) {
                content = "Thiếu dữ liệu để hoàn thiện mục này.";
                }

                item.put("noiDungChiTiet", 
                        new TextRenderData(content));

                cacMucRender.add(item);
        }

        data.put("cac_muc", cacMucRender);

        // =========================
        // TEMPLATE
        // =========================

        String templateFile =
                resolveTemplateFile(topicCode, templateCode);

        try (
                InputStream is =
                        new ClassPathResource(templateFile).getInputStream();

                ByteArrayOutputStream out =
                        new ByteArrayOutputStream()
        ) {

                // Đăng ký custom policy cho noiDungChiTiet
                Configure config = Configure.builder()
                        .bind(
                                "noiDungChiTiet",
                                new MultiLineTextRenderPolicy()
                        )
                        .build();

                XWPFTemplate template =
                        XWPFTemplate.compile(is, config)
                                .render(data);

                template.write(out);
                template.close();

                return out.toByteArray();
                }
        }

        
        // Đưa dữ liệu form vào template các field cố định
        private Map<String, Object> buildFixedDocxFields(
                DocumentFormDTO form,
                TemplateDocument.TemplateMeta meta
        ) {
                Map<String, Object> data = new HashMap<>();

                data.put("co_quan_chu_quan", upper(form.getCoQuanChuQuan()));
                data.put("co_quan_ban_hanh", upper(form.getCoQuanBanHanh()));
                data.put("so_ky_hieu", nullToEmpty(form.getSoKyHieu()));
                data.put("dia_danh", nullToEmpty(form.getDiaDanh()));
                data.put("ngay_ban_hanh", formatNgayHanhChinh(form.getNgayBanHanh()));
                data.put("ten_van_ban", upper(form.getTenVanBan()));
                data.put("noi_nhan", buildNoiNhan(form));

                String rawNguoiKy = form.getNguoiKy();
                if (rawNguoiKy != null && rawNguoiKy.contains("-")) {
                String[] parts = rawNguoiKy.split("-", 2);
                data.put("nguoi_ky", upper(parts[0].trim()));
                data.put("chuc_danh_ky", "T/M ĐẠI HỘI\n" + upper(parts[1].trim()));
                } else {
                data.put("nguoi_ky", upper(rawNguoiKy));
                data.put("chuc_danh_ky", "T/M ĐẠI HỘI\nBÍ THƯ");
                }

                data.put("ten_loai_van_ban", safe(meta.getTenLoaiVanBan()));
                data.put("cau_dan_chuyen_tiep", safe(meta.getCauDanChuyenTiep()));
                data.put("cau_ket_hanh_chinh", safe(meta.getCauKetHanhChinh()));

                return data;
        }


        // Tạo body theo cấu trúc template nếu chưa có AI
        private TemplateDocument.Body buildBodyFromWizardBySpec(
                DocumentFormDTO form,
                List<TemplateDocument.SectionSpec> specs
        ) {
                TemplateDocument.Body body = new TemplateDocument.Body();
                body.setDoanCanCu(buildDoanCanCu(form));

                List<TemplateDocument.BodySection> sections = new ArrayList<>();
                for (TemplateDocument.SectionSpec spec : specs) {
                        TemplateDocument.BodySection s = new TemplateDocument.BodySection();
                        s.setTieuDeMuc(safe(spec.getTieuDeMacDinh()));
                        s.setNoiDungChiTiet(buildSectionDetail(form, spec));
                        sections.add(s);
                }

                // if (sections.isEmpty()) {
                //         return buildBodyFromWizard(form);
                // }
                if (sections.isEmpty()) {
                        throw new IllegalStateException("Template TXT không có cấu trúc cacMuc hợp lệ. Không dùng deCuongDanY làm schema.");
                }

                body.setCacMuc(sections);

                System.out.println("=== WIZARD -> BODY ===");
                System.out.println("doanCanCu length = " +
                        (body.getDoanCanCu() == null ? 0 : body.getDoanCanCu().length()));
                System.out.println("cacMuc size = " +
                        (body.getCacMuc() == null ? 0 : body.getCacMuc().size()));
                for (int i = 0; i < body.getCacMuc().size(); i++) {
                TemplateDocument.BodySection s = body.getCacMuc().get(i);
                System.out.println("body[" + i + "].title = " + s.getTieuDeMuc());
                }
                System.out.println("======================");

                return body;
        }


        // Giải ra specs từ templateContent 
        public TemplateDocument.Body buildBodyFromWizardBySpec(
                DocumentFormDTO form,
                String templateContent
        ) {
        List<TemplateDocument.SectionSpec> specs =
                templateSpecParser.parseSectionSpecs(templateContent);
        return buildBodyFromWizardBySpec(form, specs);
        }

        // Parse JSON string từ AI về object TemplateDocument.Body >> chuyển json ai thành dto
        public TemplateDocument.Body parseBodyFromAi(String aiJsonResponse) {
                if (aiJsonResponse == null || aiJsonResponse.isBlank()) {
                        return null;
                }
                try {
                        return objectMapper.readValue(aiJsonResponse, TemplateDocument.Body.class);
                } catch (Exception e) {
                        return null;
                }
        }

        // Sửa body AI cho đúng tiêu đề và đúng số mục của template
        private TemplateDocument.Body alignBodyToSpec(
                TemplateDocument.Body aiBody,
                List<TemplateDocument.SectionSpec> specs
        ) {
        if (specs == null || specs.isEmpty()) {
                throw new IllegalStateException(
                        "Template TXT không có cấu trúc cacMuc hợp lệ."
                );
        }

        if (aiBody == null) {
                throw new IllegalStateException(
                        "AI response không hợp lệ: body null."
                );
        }

        List<TemplateDocument.BodySection> aiSections = aiBody.getCacMuc();

        if (aiSections == null) {
                throw new IllegalStateException(
                        "AI response không hợp lệ: cacMuc null."
                );
        }

        if (aiSections.size() < specs.size()) {
                throw new IllegalStateException(
                        "AI response thiếu mục: AI có "
                                + aiSections.size()
                                + " mục, template yêu cầu "
                                + specs.size()
                                + " mục."
                );
        }

        TemplateDocument.Body aligned = new TemplateDocument.Body();

        // Đoạn căn cứ lấy từ AI
        aligned.setDoanCanCu(
                safe(aiBody.getDoanCanCu())
        );

        List<TemplateDocument.BodySection> result = new ArrayList<>();

        for (int i = 0; i < specs.size(); i++) {

                TemplateDocument.SectionSpec spec = specs.get(i);
                TemplateDocument.BodySection aiSection = aiSections.get(i);

                TemplateDocument.BodySection out =
                        new TemplateDocument.BodySection();

                // TIÊU ĐỀ: luôn lấy từ TXT template
                out.setTieuDeMuc(
                        safe(spec.getTieuDeMacDinh())
                );

                // NỘI DUNG: lấy từ AI, vẫn giữ nguyên String và \n
                out.setNoiDungChiTiet(
                        safe(aiSection.getNoiDungChiTiet())
                );

                result.add(out);
        }

        aligned.setCacMuc(result);

        System.out.println("=== ALIGN BODY TO SPEC ===");
        System.out.println("spec size = " + specs.size());
        System.out.println("ai cacMuc size = " + aiSections.size());
        System.out.println("aligned cacMuc size = " + result.size());

        for (int i = 0; i < result.size(); i++) {
                TemplateDocument.BodySection section = result.get(i);

                System.out.println(
                        "aligned[" + i + "].title = "
                                + section.getTieuDeMuc()
                );

                System.out.println(
                        "aligned[" + i + "].content = "
                                + section.getNoiDungChiTiet()
                );
        }

        System.out.println("==========================");

        return aligned;
        }

        // Tạo đoạn căn cứ pháp lý
        private String buildDoanCanCu(DocumentFormDTO form) {
                String canCu = firstNotBlank(form.getVanBanChiDao(), "");
                String phapLy = firstNotBlank(form.getVanBanPhapLy(), "");
                return (canCu + (phapLy.isBlank() ? "" : "\n" + phapLy)).trim();
        }

        // Tìm template DOCX theo thứ tự
        private String resolveTemplateFile(String topicCode, String templateCode) {
                String primary =
                        "ai-resources/topics/" + topicCode + "/templates/" + templateCode + ".docx";

                boolean primaryExists = new ClassPathResource(primary).exists();
                System.out.println("template docx primary = " + primary + " | exists = " + primaryExists);

                if (primaryExists) {
                        return primary;
                }

                String sharedInTemplates =
                        "ai-resources/topics/" + topicCode + "/templates/" + topicCode + ".docx";

                boolean sharedInTemplatesExists =
                        new ClassPathResource(sharedInTemplates).exists();
                System.out.println("template docx sharedInTemplates = " + sharedInTemplates
                        + " | exists = " + sharedInTemplatesExists);

                if (sharedInTemplatesExists) {
                        return sharedInTemplates;
                }

                String sharedAtTopicRoot =
                        "ai-resources/topics/" + topicCode + "/" + topicCode + ".docx";

                boolean sharedAtTopicRootExists =
                        new ClassPathResource(sharedAtTopicRoot).exists();
                System.out.println("template docx sharedAtTopicRoot = " + sharedAtTopicRoot
                        + " | exists = " + sharedAtTopicRootExists);

                if (sharedAtTopicRootExists) {
                        return sharedAtTopicRoot;
                }

                throw new IllegalStateException("Không tìm thấy DOCX template cho topic " + topicCode);
                }
        
        // tạo phần nơi nhận
        private String buildNoiNhan(DocumentFormDTO form) {
                StringBuilder b = new StringBuilder();
                if (form.getKinhGui() != null && !form.getKinhGui().isBlank()) {
                b.append("- ").append(form.getKinhGui().trim()).append("\n");
                }
                if (form.getNoiNhanBaoCao() != null && !form.getNoiNhanBaoCao().isBlank()) {
                b.append("- ").append(form.getNoiNhanBaoCao().trim()).append("\n");
                }
                b.append("- Lưu: Văn phòng Đảng ủy.");
                return b.toString();
        }

        // định dạng ngày theo kiểu văn bản hành chính
        private String formatNgayHanhChinh(String dateStr) {
                if (dateStr == null || dateStr.isBlank()) return "ngày … tháng … năm …";
                try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return String.format("ngày %02d tháng %02d năm %d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
                } catch (Exception e) {
                return dateStr;
                }
        }

        // chọn giá trị đầu tiên không rỗng trong danh sách
        private String firstNotBlank(String... values) {
                for (String v : values) {
                if (v != null && !v.isBlank()) return v.trim();
                }
                return "";
        }

        // lấy nội dung cho từng mục
        private String buildSectionDetail(
                DocumentFormDTO form,
                TemplateDocument.SectionSpec spec
        ) {
        return firstNotBlank(
                form.getNoiDungChinh(),
                form.getBangBieuSoLieu(),
                spec.getGoiYNoiDung(),
                "Bổ sung nội dung chi tiết cho mục này."
        );
        }

        private String safe(String value) {
        return value == null ? "" : value.trim();
        }

        private String upper(String v) { return v == null ? "" : v.trim().toUpperCase(); }
        private String nullToEmpty(String v) { return v == null ? "" : v.trim(); }
}