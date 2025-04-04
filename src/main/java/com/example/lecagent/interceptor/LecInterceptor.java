package com.example.lecagent.interceptor;

import com.example.lecagent.util.JwtTool;
import com.example.lecagent.util.UserContext;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;

@Slf4j
public class LecInterceptor implements HandlerInterceptor {

    private final JwtTool jwtTool;

    public LecInterceptor(JwtTool jwtTool) {
        this.jwtTool = jwtTool;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = request.getHeader("token");
        if(token==null){
            return false;
        }
        Long userId = jwtTool.parseToken(token);
        if(userId==null){
            return false;
        }
        UserContext.setUser(userId);
        log.info("userId:{}",userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        //清理用户
        UserContext.removeUser();
    }
}
