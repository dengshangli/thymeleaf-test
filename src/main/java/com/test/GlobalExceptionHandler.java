package com.test;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseEntity<PlaygroundController.RenderResponse> handleException(Exception e) {
        // 记录异常日志（在实际项目中应该使用日志框架）
        e.printStackTrace();
        
        String errorMsg = "服务器内部错误: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
        
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PlaygroundController.RenderResponse(null, errorMsg));
    }
}

