package com.saasai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentFormDTO {

    // Menu 1
    private String loaiVanBan;
    private String tenVanBan;
    private String coQuanChuQuan;
    private String coQuanBanHanh;
    private String diaDanh;
    private String nguoiKy;
    private String soKyHieu;
    private String kinhGui;
    private String noiNhanBaoCao;
    private String ngayBanHanh;

    // Menu 2
    private String vanBanChiDao;
    private String vanBanPhapLy;

    // Menu 3
    private String noiDungChinh;
    private String bangBieuSoLieu;

    /**
     * File thực tế không gửi trực tiếp trong JSON.
     * Frontend upload trước rồi gửi fileIds.
     */
    private List<String> taiLieuMinhChungFileIds;

    // Menu 4
    private String mauVanBan;
    private String deCuongDanY;

    // Menu 5
    private String vanBanLienQuan;
    private Boolean taoPhuLuc;
    private Boolean doiChieu;
    private Boolean bamCanCu;
    private Boolean theThuc;

    // Menu 6
    private String phongCach;
    private String doDai;
    private String mucDoHoanChinh;

    /**
     * Có thể giữ để hiển thị phía frontend,
     * nhưng backend không nên tin field này để chọn model.
     */
    private String selectedModel;

    private String outputSize;
}