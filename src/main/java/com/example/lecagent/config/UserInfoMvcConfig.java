package com.example.lecagent.config;

import com.example.lecagent.interceptor.LecInterceptor;
import com.example.lecagent.util.JwtTool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Component
@Slf4j
public class UserInfoMvcConfig implements WebMvcConfigurer {
//    注册拦截器
    @Autowired
    private JwtTool jwtTool;
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new LecInterceptor(jwtTool));//这里就不再添加路径了，默认全部拦截
    }
}
