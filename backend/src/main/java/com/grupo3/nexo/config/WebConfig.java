package com.grupo3.nexo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.grupo3.nexo.usuarios.RegistroActividad;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final RegistroActividad registroActividad;

    public WebConfig(RegistroActividad registroActividad) {
        this.registroActividad = registroActividad;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registro) {
        registro.addInterceptor(registroActividad).addPathPatterns("/api/**");
    }
}
