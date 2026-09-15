package com.saasai.service;

import com.deepoove.poi.XWPFTemplate;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saasai.config.DocumentRules;
import com.saasai.dto.BlockType;
import com.saasai.dto.DocumentBlock;
import com.saasai.dto.DocumentFormDTO;
import com.saasai.dto.ExportResult;
import com.saasai.entity.ChatSession;
import com.saasai.feature.ai.AiTemplateContent;
import com.saasai.feature.ai.AiTemplateService;
import com.saasai.feature.ai.NghiQuyetExportService;
import com.saasai.feature.ai.TemplateDocument;
import com.saasai.repository.ChatSessionRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTPageMar;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSectPr;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

@Service
public class ExportService {

    private final ChatSessionRepository chatSessionRepository;
    private final DraftFileService draftFileService;
    private final ObjectMapper objectMapper;
    private final DocumentRenderService documentRenderService;
    private final DocumentRules documentRules;
    private final NghiQuyetExportService nghiQuyetExportService;
    private final AiTemplateService aiTemplateService;

    public ExportService(
            ChatSessionRepository chatSessionRepository,
            DraftFileService draftFileService,
            ObjectMapper objectMapper,
            DocumentRenderService documentRenderService,
            DocumentRules documentRules,
            NghiQuyetExportService nghiQuyetExportService,
            AiTemplateService aiTemplateService
    ) throws StreamReadException, DatabindException, IOException {
        this.chatSessionRepository = chatSessionRepository;
        this.draftFileService = draftFileService;
        this.objectMapper = objectMapper;
        this.documentRenderService = documentRenderService;
        this.documentRules =
                objectMapper.readValue(
                        getClass()
                                .getResourceAsStream("/document-rules.json"),
                        DocumentRules.class
                );
        this.nghiQuyetExportService = nghiQuyetExportService;
        this.aiTemplateService = aiTemplateService;
    }

    public ExportResult export(
        String sessionUuid,
        String format,
        String userId
) throws Exception {

    // 1. Validate
    if (sessionUuid == null) {
        throw new IllegalArgumentException("sessionUuid không được null");
    }

    if (format == null || format.isBlank()) {
        throw new IllegalArgumentException("format không được để trống");
    }

    if (userId == null || userId.isBlank()) {
        throw new IllegalArgumentException("userId không được để trống");
    }

    // 2. Lấy session + ownership
    ChatSession session =
            chatSessionRepository
                    .findBySessionUuidAndUser_UserId(sessionUuid, userId)
                    .orElseThrow(() ->
                            new NoSuchElementException(
                                    "Session không tồn tại hoặc không thuộc user"
                            )
                    );

    String status = session.getStatus().name();

    // 3. Lấy Wizard Form
    String wizardStateJson =
            draftFileService.getFormDataBySession(
                    sessionUuid,
                    userId
            );

    if (wizardStateJson == null || wizardStateJson.isBlank()) {
        throw new IllegalStateException(
                "Draft chưa có dữ liệu để export"
        );
    }

        DocumentFormDTO form = objectMapper.readValue(wizardStateJson, DocumentFormDTO.class);

        // match template bằng mauVanBan trước, fallback tenVanBan
        String routeInput = (form.getMauVanBan() != null && !form.getMauVanBan().isBlank())
                ? form.getMauVanBan()
                : form.getTenVanBan();

        // 4. Resolve template bằng AiTemplateService >> lấy mẫu 
        AiTemplateContent template = aiTemplateService.resolve(
                form.getLoaiVanBan(),
                routeInput
        );

        // 4. AI JSON
        // case 1: lấy body từ AI output đã lưu trong session (nếu có)
        // case 2: null/blank => fallback wizard body trong NghiQuyetExportService

        String aiJsonResponse = session.getEditorContent();
        

        // Check status session để quyết định cách xây dựng body
        TemplateDocument.Body body;

        System.out.println("=== EXPORT TEMPLATE ROUTE ===");
        System.out.println("status = " + session.getStatus());
        System.out.println("routeInput = " + routeInput);
        System.out.println("topicCode = " + template.topicCode());
        System.out.println("templateCode = " + template.templateCode());
        System.out.println("templateContent length = " +
                (template.templateContent() == null ? 0 : template.templateContent().length()));
        System.out.println("templateContent preview = " +
                (template.templateContent() == null ? "<null>" :
                        template.templateContent().substring(
                                0,
                                Math.min(180, template.templateContent().length())
                        )));
        System.out.println("============================");
        System.out.println("====" + session.getStatus() + " ====");
        if (session.getStatus() == ChatSession.SessionStatus.DRAFT) {
                body = nghiQuyetExportService.buildBodyFromWizardBySpec(
                        form,
                        template.templateContent()
                );
                if (body == null || body.getCacMuc() == null || body.getCacMuc().isEmpty()) {
                        throw new IllegalStateException("Không dựng được BODY từ Template TXT. Kiểm tra lại cấu trúc Mục trong file mẫu.");
                }

        } else if (session.getStatus() == ChatSession.SessionStatus.EDITING) {
                body = nghiQuyetExportService.parseBodyFromAi(
                        aiJsonResponse
                );
                if (body == null || body.getCacMuc() == null || body.getCacMuc().isEmpty()) {
                        throw new IllegalStateException("AI response không hợp lệ, không fallback sang wizard khi đã gọi AI");
                }

        } else {
        throw new IllegalStateException("Session đang ở trạng thái không hỗ trợ export: " + session.getStatus());
        }

        // 5. Render bằng DOCX template
        byte[] bytes = nghiQuyetExportService.renderNghiQuyetDocx(
                form,
                template.topicCode(),
                template.templateCode(),
                template.templateContent(),
                body
        );

        // 6. Filename
        String fileName =
                buildFileName(
                        form.getTenVanBan(),
                        format
                );

        MediaType mediaType =
                MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                );

        return new ExportResult(
                bytes,
                fileName,
                mediaType
        );
        }

//     public ExportResult export(
//             String sessionUuid,
//             String format,
//             String userId
//     ) throws Exception {
//         DocumentFormDTO form = new DocumentFormDTO();

//         form.setDoDai("MEDIUM");
//         form.setDiaDanh("Cần Thơ");
//         form.setKinhGui(
//                 "Các chi bộ trực thuộc Đảng bộ Công ty XYZ"
//         );
//         form.setNguoiKy(
//                 "Nguyễn Văn A - Bí thư Đảng ủy"
//         );

//         form.setSoKyHieu("01-NQ/ĐU");

//         form.setTenVanBan(
//                 "Nghị quyết Đại hội đại biểu Đảng bộ Công ty XYZ lần thứ XX, nhiệm kỳ 2025-2030"
//         );

//         form.setNgayBanHanh("21/08/2026");

//         form.setCoQuanBanHanh(
//                 "Đảng bộ Công ty XYZ"
//         );

//         form.setCoQuanChuQuan(
//                 "Công ty XYZ"
//         );

//         form.setNoiNhanBaoCao(
//                 "Ban Thường vụ cấp trên và các đơn vị liên quan"
//         );
//         form.setTheThuc(true);
//         form.setBamCanCu(true);
//         form.setDoiChieu(false);

//         form.setMauVanBan(
//                 "Mẫu Nghị quyết Đại hội đại biểu Đảng bộ nhiệm kỳ 2025-2030"
//         );

//         form.setPhongCach(
//                 "Trang trọng, chính luận, rõ ràng, đúng thể thức văn bản hành chính"
//         );

//         form.setTaoPhuLuc(false);

//         form.setLoaiVanBan("NGHI_QUYET");
//         form.setOutputSize("MEDIUM");

//         form.setDeCuongDanY(
//                 "I. Đánh giá kết quả thực hiện nhiệm kỳ trước; " +
//                 "II. Phương hướng và mục tiêu nhiệm kỳ mới; " +
//                 "III. Nhiệm vụ và giải pháp; " +
//                 "IV. Tổ chức thực hiện."
//         );

//         form.setNoiDungChinh(
//                 "Đánh giá kết quả thực hiện nhiệm vụ nhiệm kỳ 2020-2025; " +
//                 "xác định phương hướng, mục tiêu, nhiệm vụ và giải pháp chủ yếu " +
//                 "nhiệm kỳ 2025-2030. Tập trung nâng cao năng lực lãnh đạo của tổ chức Đảng, " +
//                 "chất lượng đội ngũ cán bộ, đảng viên, hiệu quả hoạt động sản xuất kinh doanh " +
//                 "và công tác chuyển đổi số."
//         );

//         form.setVanBanChiDao(
//                 "Căn cứ Điều lệ Đảng Cộng sản Việt Nam; " +
//                 "Căn cứ nghị quyết và các văn bản chỉ đạo của cấp trên; " +
//                 "Căn cứ tình hình thực tế của Đảng bộ Công ty XYZ."
//         );

//         form.setVanBanPhapLy(
//                 "Căn cứ các quy định, hướng dẫn hiện hành có liên quan " +
//                 "đến công tác xây dựng Đảng và hoạt động của doanh nghiệp."
//         );

//         form.setBangBieuSoLieu(
//                 "Doanh thu tăng trưởng bình quân 10% mỗi năm; " +
//                 "100% chi bộ hoàn thành nhiệm vụ; " +
//                 "trên 90% đảng viên hoàn thành tốt nhiệm vụ."
//         );

//         form.setMucDoHoanChinh("FULL");

//         form.setVanBanLienQuan(
//                 "Nghị quyết Đại hội nhiệm kỳ 2020-2025; " +
//                 "các nghị quyết, chỉ thị và văn bản hướng dẫn của cấp trên."
//         );


//         String aiJsonResponse = """
// {
//         "doanCanCu": "Thực hiện căn cứ Điều lệ Đảng Cộng sản Việt Nam...",
//         "cacMuc": [
//         {
//         "tieuDeMuc": "I. Tán thành những nội dung cơ bản",
//         "noiDungChiTiet": "1. Kết quả đạt được:\\n- Doanh thu tăng 8%."
//         },
//         {
//         "tieuDeMuc": "II. Phương hướng, mục tiêu",
//         "noiDungChiTiet": "1. Phương hướng:\\n- Tiếp tục nâng cao năng lực lãnh đạo."
//         }
//         ]
//         }
//         """;

//         System.out.println("=== START RENDER DOCX ===");
//         byte[] result =
//                 nghiQuyetExportService.renderNghiQuyetDocx(
//                         form,
//                         aiJsonResponse
//                 );

//         System.out.println("========== DOCX TEST ==========");
//         System.out.println("DOCX SIZE = " + result.length + " bytes");
//         System.out.println("DOCX SIZE = " + (result.length / 1024) + " KB");
//         System.out.println("FORM tenVanBan = " + form.getTenVanBan());
//         System.out.println("FORM soKyHieu = " + form.getSoKyHieu());
//         System.out.println("AI JSON LENGTH = " + aiJsonResponse.length());
//         System.out.println("================================");

       

//         // 1. Validate input
//         if (sessionUuid == null) {
//             throw new IllegalArgumentException("sessionUuid không được null");
//         }

//         if (format == null || format.isBlank()) {
//             throw new IllegalArgumentException("format không được để trống");
//         }

//         if (userId == null || userId.isBlank()) {
//             throw new IllegalArgumentException("userId không được để trống");
//         }

//         // 2. Lấy session + kiểm tra ownership
//         ChatSession session = chatSessionRepository
//                 .findBySessionUuidAndUser_UserId(sessionUuid, userId)
//                 .orElseThrow(() ->
//                         new NoSuchElementException(
//                                 "Session không tồn tại hoặc không thuộc user"
//                         )
//                 );

//         Integer sessionId = session.getSessionId();

//         String status = session.getStatus().name();

//         System.out.println("=============== Status ==================");
//         System.out.println("Status = " + status);
//         String documentText;
//         String documentName;
       

//         // 3. DRAFT → render từ wizard_state_json (use DraftFileService abstraction)
//         if ("DRAFT".equalsIgnoreCase(status)) {

//             String wizardStateJson = draftFileService.getFormDataBySession(sessionUuid, userId);

//             if (wizardStateJson == null || wizardStateJson.isBlank()) {
//                 throw new IllegalStateException("Draft chưa có dữ liệu để export");
//             }

//             try {
//                 DocumentFormDTO formData = objectMapper.readValue(wizardStateJson, DocumentFormDTO.class);

//                 // Use shared renderer (reuse template + prompt builder)
//                 documentText = documentRenderService.render(formData);

//                 documentName = formData.getTenVanBan();

//             } catch (Exception e) {
//                 throw new IllegalStateException("Không thể đọc wizard_state_json", e);
//             }

//         }
//         // 4. EDITING → lấy editor_content (Redis → DB fallback)
//         else if ("EDITING".equalsIgnoreCase(status)) {

//             // Nội dung hiện tại để export = kết quả AI / nội dung người dùng chỉnh sửa
//             documentText = draftFileService.getEditorTextBySession(
//                     sessionUuid,
//                     userId
//             );

//             if (documentText == null || documentText.isBlank()) {
//                 throw new IllegalStateException(
//                         "Editor chưa có nội dung để export"
//                 );
//             }

//             // Parse  chuẩn hóa text thành các DocumentBlock
//                 List<DocumentBlock> blocks =
//                         documentRenderService.parse(documentText);

//                 // System.out.println("========== Test1 =========");
//                 // System.out.println(blocks);

//             // Lấy formData cũ chỉ để lấy metadata, đặc biệt là tên văn bản
//             String wizardStateJson =
//                     draftFileService.getFormDataBySession(
//                             sessionUuid,
//                             userId
//                     );

//             if (wizardStateJson != null && !wizardStateJson.isBlank()) {

//                 try {
//                     DocumentFormDTO formData =
//                             objectMapper.readValue(
//                                     wizardStateJson,
//                                     DocumentFormDTO.class
//                             );

//                     documentName = formData.getTenVanBan();

//                 } catch (Exception e) {
//                     throw new IllegalStateException(
//                             "Không thể đọc formData của session",
//                             e
//                     );
//                 }
//             } else {
//                 documentName = null;
//             }

//             // Fallback nếu không còn formData
//             if (documentName == null || documentName.isBlank()) {
//                 documentName = session.getSessionName();
//             }

//         }
//         // 5. Status khác → lỗi
//         else {
//             throw new IllegalStateException("Session đang ở trạng thái không thể export: " + status);
//         }

//         // 6. Tạo filename
//         String fileName = buildFileName(documentName, format);

//         // Parse  chuẩn hóa text thành các DocumentBlock
//         List<DocumentBlock> blocks =
//             documentRenderService.parse(documentText);

//                 System.out.println("========== Test =========");
//                 System.out.println(blocks);

//         // 7. Convert content → bytes (DOCX / PDF / TXT)
//         return buildExportResult(blocks, fileName, format);
//     }

    private String buildFileName(String documentName, String format) {
        String name = documentName;
        if (name == null || name.isBlank()) {
            name = "van_ban";
        }
        name = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        name = name.replace("đ", "d").replace("Đ", "D");
        name = name.replaceAll("[^a-zA-Z0-9_-]", "_");
        name = name.replaceAll("_+", "_");
        if (name.length() > 100) name = name.substring(0, 100);
        name = name.replaceAll("^_+|_+$", "");
        String extension = format.trim().toLowerCase(Locale.ROOT);
        return name + "." + extension;
    }

    private ExportResult buildExportResult(
        List<DocumentBlock> blocks,
        String fileName,
        String format
    ) {
        String normalizedFormat = format.trim().toUpperCase(Locale.ROOT);

        byte[] bytes;
        MediaType mediaType;

        switch (normalizedFormat) {
            case "TXT":
                bytes = blocks.stream()
                        .map(DocumentBlock::text)
                        .collect(Collectors.joining("\n"))
                        .getBytes(StandardCharsets.UTF_8);

                mediaType = MediaType.TEXT_PLAIN;
                break;

            case "DOCX":
                bytes = toDocxBytes(blocks);
                mediaType = MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                );
                break;

            case "PDF":
                bytes = toPdfBytes(blocks);
                mediaType = MediaType.APPLICATION_PDF;
                break;

            default:
                throw new IllegalArgumentException(
                        "Format không hỗ trợ: " + format
                );
        }

        return new ExportResult(bytes, fileName, mediaType);
    }

    private byte[] toDocxBytes(List<DocumentBlock> blocks) {

        try (
                XWPFDocument doc = new XWPFDocument();
                ByteArrayOutputStream out = new ByteArrayOutputStream()
        ) {

            DocumentRules.PageRule page = documentRules.page;

            // =========================
            // PAGE MARGIN
            // =========================
            CTSectPr sectPr =
                    doc.getDocument()
                            .getBody()
                            .addNewSectPr();

            CTPageMar pageMar =
                    sectPr.addNewPgMar();

            pageMar.setLeft(
                    BigInteger.valueOf(
                            mmToTwips(page.margin.leftMm)
                    )
            );

            pageMar.setRight(
                    BigInteger.valueOf(
                            mmToTwips(page.margin.rightMm)
                    )
            );

            pageMar.setTop(
                    BigInteger.valueOf(
                            mmToTwips(page.margin.topMm)
                    )
            );

            pageMar.setBottom(
                    BigInteger.valueOf(
                            mmToTwips(page.margin.bottomMm)
                    )
            );

            // =========================
            // DOCUMENT CONTENT
            // =========================
            for (DocumentBlock block : blocks) {

            BlockType type = block.type();

            // =====================================================
            // HEADER / HEADER_META: render dạng 2 cột
            // =====================================================
            if (type == BlockType.RECIPIENT_SIGNATURE) {
            renderTwoColumns(
                    doc,
                    block.leftText(),
                    block.rightText(),
                    ParagraphAlignment.LEFT,
                    ParagraphAlignment.RIGHT,
                    (int) documentRules.recipientSignature.leftWidth,
                    (int) documentRules.recipientSignature.rightWidth
            );

            continue;
        }

            if (type == BlockType.HEADER || type == BlockType.HEADER_META
            ) {

            renderTwoColumns(
                    doc,
                    block.leftText(),
                    block.rightText(),
                    ParagraphAlignment.LEFT,
                    ParagraphAlignment.RIGHT,
                    (int) documentRules.header.leftWidth,
                    (int) documentRules.header.rightWidth
            );
            
            continue;
        }



            String text = block.text();
                
            XWPFParagraph paragraph = doc.createParagraph();

            // spacing
            paragraph.setSpacingAfter(
                    (int) documentRules.body.paragraphSpacingPt
            );

            paragraph.setSpacingBetween(
                    documentRules.body.lineHeight
            );

            // alignment + font theo block type
            switch (type) {

                case TITLE:
                    paragraph.setAlignment(
                            getAlignment(documentRules.title.alignment)
                    );
                    break;

                case TITLE_SUB:
                    paragraph.setAlignment(
                            getAlignment(documentRules.title.alignment)
                    );
                    break;

                case HEADING:
                    paragraph.setAlignment(
                            getAlignment(documentRules.heading.alignment)
                    );
                    break;

                case SECTION_HEADING:
                    paragraph.setAlignment(
                            getAlignment(documentRules.sectionHeading.alignment)
                    );
                    break;

                case RECIPIENT:
                    paragraph.setAlignment(
                            getAlignment(documentRules.recipient.layout)
                    );
                    break;

                case SIGNATURE:
                    paragraph.setAlignment(
                            getAlignment(documentRules.signature.layout)
                    );
                    break;

                case BODY:
                default:
                    paragraph.setAlignment(
                            getAlignment(documentRules.body.alignment)
                    );
                    break;
            }

            XWPFRun run = paragraph.createRun();

            run.setFontFamily(documentRules.font.family);

            switch (type) {

                case TITLE:
                    run.setFontSize(
                            (int) documentRules.title.fontSize
                    );
                    run.setBold(
                            documentRules.title.bold
                    );
                    break;

                case TITLE_SUB:
                    run.setFontSize(
                            (int) documentRules.title.fontSize
                    );
                    run.setBold(
                            documentRules.title.bold
                    );
                    break;

                case HEADING:
                    run.setFontSize(
                            (int) documentRules.heading.fontSize
                    );
                    run.setBold(
                            documentRules.heading.bold
                    );
                    break;

                case SECTION_HEADING:
                    run.setFontSize(
                            (int) documentRules.sectionHeading.fontSize
                    );
                    run.setBold(
                            documentRules.sectionHeading.bold
                    );
                    break;

                case BODY:
                case RECIPIENT:
                case SIGNATURE:
                default:
                    run.setFontSize(
                            (int) documentRules.body.fontSize
                    );
                    run.setBold(false);
                    break;
            }

            run.setText(text);
        }
        // QUAN TRỌNG: phải có return ở đây
            doc.write(out);

            return out.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Lỗi khi tạo DOCX",
                    e
            );
        }
    }

    private void renderTwoColumns(
        XWPFDocument doc,
        String leftText,
        String rightText,
        ParagraphAlignment leftAlignment,
        ParagraphAlignment rightAlignment,
        int leftWidth,
        int rightWidth
        
    ) {

        // Tạo 2 cột
        XWPFTable table = doc.createTable(1, 2);

        table.getCTTbl()
                .getTblPr()
                .unsetTblBorders();

        XWPFTableCell leftCell =
                table.getRow(0).getCell(0);

        XWPFTableCell rightCell =
                table.getRow(0).getCell(1);

        leftCell.setWidth(leftWidth + "%");
        rightCell.setWidth(rightWidth + "%");

        renderCell(
            leftCell,
            leftText,
            leftAlignment
        );

        renderCell(
                rightCell,
                rightText,
                rightAlignment
        );
    }

    private void renderCell(
        XWPFTableCell cell,
        String text,
        ParagraphAlignment alignment
    ) {

        // paragraph mặc định của cell
        XWPFParagraph paragraph =
                cell.getParagraphs().get(0);

        paragraph.setAlignment(alignment);

        String[] lines =
                text == null
                        ? new String[0]
                        : text.split("\\r?\\n");

        for (int i = 0; i < lines.length; i++) {

            XWPFParagraph p;

            if (i == 0) {
                p = paragraph;
            } else {
                p = cell.addParagraph();
            }

            p.setAlignment(alignment);

            XWPFRun run = p.createRun();

            run.setFontFamily(
                    documentRules.font.family
            );

            run.setFontSize(
                    (int) documentRules.body.fontSize
            );

            run.setText(lines[i]);
        }
    }

    private long mmToTwips(double mm) {
        return Math.round(mm * 56.6929);
    }
    private boolean isTitle(String text) {
        return text.startsWith("## ")
                || text.equals("NGHỊ QUYẾT")
                || text.contains("ĐẠI HỘI ĐẠI BIỂU");
    }
    private boolean isHeading(String text) {
        return text.startsWith("### ")
                || text.matches("^[IVX]+\\..*")
                || text.equals("QUYẾT NGHỊ")
                || text.equals("CĂN CỨ")
                || text.equals("NỘI DUNG CHÍNH")
                || text.equals("MẪU")
                || text.equals("TUỲ CHỌN")
                || text.equals("YÊU CẦU ĐẦU RA");
    }

    private byte[] toPdfBytes(List<DocumentBlock> blocks) {
        try (
                PDDocument pdf = new PDDocument();
                ByteArrayOutputStream baos = new ByteArrayOutputStream()
        ) {
            DocumentRules.PageRule pageRule = documentRules.page;

            float leftMargin =
                    mmToPoints(pageRule.margin.leftMm);

            float rightMargin =
                    mmToPoints(pageRule.margin.rightMm);

            float topMargin =
                    mmToPoints(pageRule.margin.topMm);

            float bottomMargin =
                    mmToPoints(pageRule.margin.bottomMm);

            PDRectangle pageSize = PDRectangle.A4;

            float pageWidth =
                    pageSize.getWidth();

            float pageHeight =
                    pageSize.getHeight();

            float contentWidth =
                    pageWidth - leftMargin - rightMargin;


            // =========================
            // FONT
            // =========================
            PDType0Font regularFont =
                    PDType0Font.load(
                            pdf,
                            getClass().getResourceAsStream(
                                    "/fonts/NotoSans-Regular.ttf"
                            )
                    );

            PDType0Font boldFont =
                    PDType0Font.load(
                            pdf,
                            getClass().getResourceAsStream(
                                    "/fonts/NotoSans-Bold.ttf"
                            )
                    );


            // =========================
            // FIRST PAGE
            // =========================
            PDPage page =
                    new PDPage(pageSize);

            pdf.addPage(page);

            PDPageContentStream cs =
                    new PDPageContentStream(pdf, page);

            float y =
                    pageHeight - topMargin;


            // =========================
            // DOCUMENT BLOCKS
            // =========================
            for (DocumentBlock block : blocks) {

                String text = block.text();

                if (text == null || text.isBlank()) {
                    continue;
                }

                text = text.trim();

                BlockType type =
                        block.type();


                // =========================
                // STYLE
                // =========================
                float fontSize;
                boolean bold;
                String alignment;

                switch (type) {

                    case TITLE:

                        fontSize =
                                (float) documentRules.title.fontSize;

                        bold =
                                documentRules.title.bold;

                        alignment =
                                documentRules.title.alignment;

                        break;


                    case HEADING:

                        fontSize =
                                (float) documentRules.heading.fontSize;

                        bold =
                                documentRules.heading.bold;

                        alignment =
                                documentRules.heading.alignment;

                        break;


                    case SECTION_HEADING:

                        fontSize =
                                (float) documentRules.sectionHeading.fontSize;

                        bold =
                                documentRules.sectionHeading.bold;

                        alignment =
                                documentRules.sectionHeading.alignment;

                        break;


                    case RECIPIENT:

                        fontSize =
                                (float) documentRules.body.fontSize;

                        bold = false;
                        alignment =
                                documentRules.recipient.layout;

                        break;

                    case SIGNATURE:

                        fontSize =
                                (float) documentRules.body.fontSize;

                        bold = false;
                        alignment =
                                documentRules.signature.layout;

                        break;

                    case BODY:

                    default:

                        fontSize =
                                (float) documentRules.body.fontSize;

                        bold = false;

                        alignment =
                                documentRules.body.alignment;

                        break;
                }


                PDType0Font font =
                        bold
                                ? boldFont
                                : regularFont;


                // =========================
                // WRAP TEXT
                // =========================
                List<String> lines =
                        wrapText(
                                text,
                                font,
                                fontSize,
                                contentWidth
                        );


                float lineHeight =
                        fontSize
                                * (float) documentRules.body.lineHeight;


                float paragraphSpacing =
                        (float) documentRules.body.paragraphSpacingPt;


                // =========================
                // DRAW EACH LINE
                // =========================
                for (String line : lines) {

                    // Nếu không đủ chỗ cho dòng tiếp theo
                    if (y < bottomMargin + lineHeight) {

                        cs.close();

                        page =
                                new PDPage(pageSize);

                        pdf.addPage(page);

                        cs =
                                new PDPageContentStream(
                                        pdf,
                                        page
                                );

                        y =
                                pageHeight - topMargin;
                    }

                    float textWidth =
                            font.getStringWidth(line)
                                    / 1000f
                                    * fontSize;


                    // =========================
                    // ALIGNMENT
                    // =========================

                    float x;

                    switch (
                            alignment.toUpperCase(Locale.ROOT)
                    ) {

                        case "CENTER":

                            x =
                                    leftMargin
                                            + (contentWidth - textWidth) / 2f;

                            break;

                        case "RIGHT":

                            x =
                                    pageWidth
                                            - rightMargin
                                            - textWidth;

                            break;


                        case "LEFT":

                            x =
                                    leftMargin;

                            break;


                        case "JUSTIFY":

                            // Với PDF hiện tại,
                            // justify sẽ xử lý ở helper riêng.
                            x =
                                    leftMargin;

                            break;

                        default:

                            x =
                                    leftMargin;
                    }

                    // =========================
                    // DRAW LINE
                    // =========================
                    cs.beginText();

                    cs.setFont(
                            font,
                            fontSize
                    );

                    cs.newLineAtOffset(
                            x,
                            y
                    );

                    cs.showText(line);

                    cs.endText();


                    y -= lineHeight;
                }


                // =========================
                // PARAGRAPH SPACING
                // =========================
                y -= paragraphSpacing;
            }


            cs.close();

            pdf.save(baos);

            return baos.toByteArray();

        } catch (IOException e) {

            throw new IllegalStateException(
                    "Lỗi khi tạo PDF",
                    e
            );
        }
    }

    private List<String> wrapText(
        String text,
        PDFont font,
        float fontSize,
        float maxWidth
    ) throws IOException {

        List<String> lines =
                new ArrayList<>();

        String[] words =
                text.split("\\s+");

        StringBuilder currentLine =
                new StringBuilder();

        for (String word : words) {

            String candidate;

            if (currentLine.isEmpty()) {
                candidate = word;
            } else {
                candidate =
                        currentLine
                                + " "
                                + word;
            }

            float width =
                    font.getStringWidth(candidate)
                            / 1000f
                            * fontSize;

            if (width <= maxWidth) {

                currentLine =
                        new StringBuilder(candidate);

            } else {

                if (!currentLine.isEmpty()) {

                    lines.add(
                            currentLine.toString()
                    );
                }

                currentLine =
                        new StringBuilder(word);
            }
        }

        if (!currentLine.isEmpty()) {

            lines.add(
                    currentLine.toString()
            );
        }

        return lines;
    }

    private float mmToPoints(double mm) {
        return (float) (mm * 72.0 / 25.4);
    }
    private float getTextWidth(
            String text,
            double fontSize,
            PDType0Font font
    ) throws IOException {

        return font.getStringWidth(
                removeMarkdown(text)
        ) / 1000f
                * (float) fontSize;
    }
    private String removeMarkdown(String text) {
        return text
                .replaceFirst("^#{1,6}\\s*", "")
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("\\*(.*?)\\*", "$1");
    }

    private ParagraphAlignment getAlignment(String alignment) {
        return switch (alignment.toUpperCase(Locale.ROOT)) {

            case "CENTER" -> ParagraphAlignment.CENTER;
            case "RIGHT" -> ParagraphAlignment.RIGHT;
            case "LEFT" -> ParagraphAlignment.LEFT;
            case "JUSTIFY" -> ParagraphAlignment.BOTH;

            default -> ParagraphAlignment.LEFT;
        };
    }
}
