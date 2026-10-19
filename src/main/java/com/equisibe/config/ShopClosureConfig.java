package com.equisibe.config;

import com.equisibe.repository.ShopSettingsRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ShopClosureConfig implements WebMvcConfigurer {
    private final ShopSettingsRepository settings;

    public ShopClosureConfig(ShopSettingsRepository settings) {
        this.settings = settings;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(
                    HttpServletRequest request,
                    HttpServletResponse response,
                    Object handler) {
                String path = request.getRequestURI()
                        .substring(request.getContextPath().length());
                String method = request.getMethod();

                boolean catalog = method.equals("GET")
                        && (path.equals("/api/products")
                        || path.startsWith("/api/products/"));

                boolean purchase = method.equals("POST")
                        && (path.equals("/api/orders")
                        || path.equals("/api/orders/")
                        || path.startsWith("/api/checkout/")
                        || path.matches("/api/orders/[^/]+/checkout-session/?"));

                if ((catalog || purchase)
                        && settings.findById(1L)
                                .map(item -> item.isClosed()).orElse(false)) {
                    throw new ResponseStatusException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "La tienda está cerrada temporalmente."
                    );
                }
                return true;
            }
        }).addPathPatterns("/api/**");
    }
}
