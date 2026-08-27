package wheelgame.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")  // 覆盖所有接口
                .allowedOrigins("http://localhost:5173") // 允许您前端开发的地址
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS") // 显式允许 PUT 和 OPTIONS
                .allowedHeaders("*")  // 允许所有请求头
                .allowCredentials(true); // 如果涉及到 Cookie 或认证，可以打开
    }
}