package com.saasai.feature.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TemplateDocument {

    private String topicCode;
    private String templateCode;
    private Map<String, Object> fixedFields;
    private Body body;

    public String getTopicCode() { return topicCode; }
    public void setTopicCode(String topicCode) { this.topicCode = topicCode; }

    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    public Map<String, Object> getFixedFields() { return fixedFields; }
    public void setFixedFields(Map<String, Object> fixedFields) { this.fixedFields = fixedFields; }

    public Body getBody() { return body; }
    public void setBody(Body body) { this.body = body; }

    public static class Body {
        private String doanCanCu;
        private List<BodySection> cacMuc = new ArrayList<>();

        public String getDoanCanCu() { return doanCanCu; }
        public void setDoanCanCu(String doanCanCu) { this.doanCanCu = doanCanCu; }

        public List<BodySection> getCacMuc() { return cacMuc; }
        public void setCacMuc(List<BodySection> cacMuc) { this.cacMuc = cacMuc; }
    }

    public static class BodySection {
        private String tieuDeMuc;
        private String noiDungChiTiet;

        public String getTieuDeMuc() { return tieuDeMuc; }
        public void setTieuDeMuc(String tieuDeMuc) { this.tieuDeMuc = tieuDeMuc; }

        public String getNoiDungChiTiet() { return noiDungChiTiet; }
        public void setNoiDungChiTiet(String noiDungChiTiet) { this.noiDungChiTiet = noiDungChiTiet; }
    }

    public static class TemplateMeta {
    private String tenLoaiVanBan;
    private String cauDanChuyenTiep;
    private String cauKetHanhChinh;
    
        public String getTenLoaiVanBan() { return tenLoaiVanBan; }
        public void setTenLoaiVanBan(String tenLoaiVanBan) { this.tenLoaiVanBan = tenLoaiVanBan; }
        public String getCauDanChuyenTiep() { return cauDanChuyenTiep; }
        public void setCauDanChuyenTiep(String cauDanChuyenTiep) { this.cauDanChuyenTiep = cauDanChuyenTiep; }
        public String getCauKetHanhChinh() { return cauKetHanhChinh; }
        public void setCauKetHanhChinh(String cauKetHanhChinh) { this.cauKetHanhChinh = cauKetHanhChinh; }
    }

    public static class SectionSpec {
        private String tieuDeMacDinh;
        private String goiYNoiDung;
        
        public String getTieuDeMacDinh() { return tieuDeMacDinh; }
        public void setTieuDeMacDinh(String tieuDeMacDinh) { this.tieuDeMacDinh = tieuDeMacDinh; }
        public String getGoiYNoiDung() { return goiYNoiDung; }
        public void setGoiYNoiDung(String goiYNoiDung) { this.goiYNoiDung = goiYNoiDung; }
    
    }

    private TemplateMeta meta;
    private List<SectionSpec> sectionSpecs;

    public TemplateMeta getMeta() { return meta; }
    public void setMeta(TemplateMeta meta) { this.meta = meta; }
    public List<SectionSpec> getSectionSpecs() { return sectionSpecs; }
    public void setSectionSpecs(List<SectionSpec> sectionSpecs) {this.sectionSpecs = sectionSpecs;}

}

