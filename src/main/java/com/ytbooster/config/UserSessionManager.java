package com.ytbooster.config;

import lombok.extern.slf4j.Slf4j;  // ✅ Lombok's Slf4j annotation
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;
import com.ytbooster.service.UserService;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Slf4j  // ✅ This automatically creates a 'log' field
@Component
public class UserSessionManager implements HttpSessionListener, 
                                          ApplicationListener<AuthenticationSuccessEvent> {
    
    @Autowired
    private UserService userService;
    
    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        String email = event.getAuthentication().getName();
        try {
            userService.setUserOnline(email, true);
            log.info("User {} set to online", email);  // ✅ 'log' provided by @Slf4j
        } catch (Exception e) {
            log.error("Failed to set user online: {}", email, e);
        }
    }
    
    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        Object principal = se.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        if (principal != null) {
            log.info("Session destroyed for user");
        }
    }
}
