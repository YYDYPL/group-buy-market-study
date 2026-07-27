package com.hjs.study.trigger.http;

import com.hjs.study.api.dto.UploadImageResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.types.enums.ResponseCode;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 运营后台商品图片上传接口。
 */
@RestController
@RequestMapping("/api/v1/gbm/admin")
public class AdminUploadController {

    private static final long MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png", "webp"));

    @Value("${gbm.admin.upload-dir:./uploads}")
    private String uploadDir;

    @PostMapping("/images")
    public Response<UploadImageResponseDTO> upload(@RequestParam("file") MultipartFile file) {
        try {
            validate(file);
            String extension = extension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path basePath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(basePath);
            Path target = basePath.resolve(fileName).normalize();
            if (!target.startsWith(basePath)) throw new IllegalArgumentException("非法文件路径");
            file.transferTo(target.toFile());
            return Response.<UploadImageResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(UploadImageResponseDTO.builder().fileName(fileName).url("/uploads/" + fileName).build())
                    .build();
        } catch (IllegalArgumentException e) {
            return Response.<UploadImageResponseDTO>builder()
                    .code(ResponseCode.ILLEGAL_PARAMETER.getCode()).info(e.getMessage()).build();
        } catch (Exception e) {
            return Response.<UploadImageResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode()).info("图片上传失败").build();
        }
    }

    private void validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) throw new IllegalArgumentException("请选择图片文件");
        if (file.getSize() > MAX_SIZE) throw new IllegalArgumentException("图片不能超过5MB");
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(ext)) throw new IllegalArgumentException("仅支持JPG、PNG和WebP图片");
        String contentType = file.getContentType();
        if (StringUtils.isBlank(contentType) || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new IllegalArgumentException("文件类型不是图片");
        }
        byte[] header = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(header);
        }
        boolean jpeg = length >= 3 && (header[0] & 0xff) == 0xff && (header[1] & 0xff) == 0xd8 && (header[2] & 0xff) == 0xff;
        boolean png = length >= 8 && (header[0] & 0xff) == 0x89 && header[1] == 0x50 && header[2] == 0x4e && header[3] == 0x47;
        boolean webp = length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
        if (!jpeg && !png && !webp) throw new IllegalArgumentException("图片文件签名不合法");
        boolean jpegFile = ("jpg".equals(ext) || "jpeg".equals(ext))
                && ("image/jpeg".equalsIgnoreCase(contentType) || "image/jpg".equalsIgnoreCase(contentType))
                && jpeg;
        boolean pngFile = "png".equals(ext) && "image/png".equalsIgnoreCase(contentType) && png;
        boolean webpFile = "webp".equals(ext) && "image/webp".equalsIgnoreCase(contentType) && webp;
        if (!jpegFile && !pngFile && !webpFile) {
            throw new IllegalArgumentException("图片扩展名、Content-Type和文件内容不一致");
        }
    }

    private String extension(String name) {
        if (StringUtils.isBlank(name) || !name.contains(".")) throw new IllegalArgumentException("图片缺少扩展名");
        return name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
