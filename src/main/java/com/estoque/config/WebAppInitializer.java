package com.estoque.config;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.WebApplicationInitializer;
import org.springframework.web.context.ContextLoaderListener;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

public class WebAppInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        // Perfil ativo: variável de ambiente SPRING_PROFILES_ACTIVE (h2 por padrão em dev local).
        String activeProfile = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "h2");
        servletContext.setInitParameter("spring.profiles.active", activeProfile);

        // Contexto raiz: persistência, serviços, segurança — compartilhado entre REST e JSF.
        AnnotationConfigWebApplicationContext rootContext = new AnnotationConfigWebApplicationContext();
        rootContext.getEnvironment().setActiveProfiles(activeProfile);
        rootContext.register(PersistenceConfig.class, FlywayConfig.class, SecurityConfig.class, OpenApiConfig.class);
        servletContext.addListener(new ContextLoaderListener(rootContext));

        // Contexto filho REST: só os @RestController, herda os beans do contexto raiz.
        AnnotationConfigWebApplicationContext restContext = new AnnotationConfigWebApplicationContext();
        restContext.register(WebConfig.class);
        ServletRegistration.Dynamic dispatcher = servletContext.addServlet("dispatcher", new DispatcherServlet(restContext));
        dispatcher.setLoadOnStartup(1);
        dispatcher.addMapping("/");
    }
}
