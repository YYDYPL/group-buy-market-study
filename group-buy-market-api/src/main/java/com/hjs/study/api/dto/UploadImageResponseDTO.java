package com.hjs.study.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品图片上传响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UploadImageResponseDTO {

    private String fileName;
    private String url;
}
