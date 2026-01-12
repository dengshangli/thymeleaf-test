package com.test;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@Controller
public class EmailTestApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(EmailTestApplication.class, args);
    }
    
    @GetMapping("/test-email")
    public String testEmail(Model model) {
        // 在这里添加你的测试数据
        model.addAttribute("userName", "张三");
        model.addAttribute("email", "test@example.com");
        model.addAttribute("orderId", "12345");
        model.addAttribute("items", Arrays.asList("项目1", "项目2", "项目3"));
        
        // 返回模板文件名（不需要 .html 后缀）
        return "email-template";
    }
}