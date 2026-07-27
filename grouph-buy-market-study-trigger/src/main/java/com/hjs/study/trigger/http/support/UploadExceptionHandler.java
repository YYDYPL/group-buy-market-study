package com.hjs.study.trigger.http.support;

import com.hjs.study.api.dto.UploadImageResponseDTO;
import com.hjs.study.api.response.Response;
import com.hjs.study.types.enums.ResponseCode;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 上传请求在进入控制器之前发生的异常处理。
 *
 * <p>Multipart 大小限制由 Spring 在解析请求体时执行，因此超限文件不会进入
 * {@code AdminUploadController}。这里把框架异常转换为统一业务响应，让后台页面
 * 能明确提示“不能超过 5MB”，而不是只显示无法理解的 HTTP 500。</p>
 */
@RestControllerAdvice
public class UploadExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public Response<UploadImageResponseDTO> handleMaxUploadSize() {
        return Response.<UploadImageResponseDTO>builder()
                .code(ResponseCode.ILLEGAL_PARAMETER.getCode())
                .info("图片大小不能超过5MB")
                .build();
    }
}
