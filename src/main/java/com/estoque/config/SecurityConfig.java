package com.estoque.config;

import com.estoque.security.JwtAuthFilter;
import com.estoque.security.RestAccessDeniedHandler;
import com.estoque.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // Publicadas em todos os perfis, inclusive produção: login é sempre público, e expor a
    // documentação OpenAPI/Swagger para consumidores da API é uma escolha deliberada e comum
    // em APIs REST públicas.
    //
    // AntPathRequestMatcher (não requestMatchers(String...) via MvcRequestMatcher) porque esta
    // SecurityFilterChain guarda tanto /api/** quanto, a partir da Task 10, /faces/** — o FacesServlet
    // do JSF nunca passa pelo DispatcherServlet/Spring MVC. MvcRequestMatcher exige um bean
    // mvcHandlerMappingIntrospector que só existe no contexto filho do DispatcherServlet (WebConfig),
    // mas SecurityConfig vive no contexto raiz (WebAppInitializer), compartilhado entre REST e JSF —
    // então esse bean não é visível aqui em runtime real (só nos testes, que carregam WebConfig e
    // SecurityConfig juntos num único contexto achatado). AntPathRequestMatcher casa
    // getServletPath()+getPathInfo() diretamente, sem depender do Spring MVC.
    private static final RequestMatcher[] ROTAS_PUBLICAS = {
            new AntPathRequestMatcher("/api/v1/auth/login"),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**")
    };

    private static final RequestMatcher ROTA_H2_CONSOLE = new AntPathRequestMatcher("/h2-console/**");

    private final JwtAuthFilter jwtAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;
    private final UserDetailsService userDetailsService;
    private final Environment environment;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        boolean producao = environment.matchesProfiles("oracle");

        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> {
                    // O console H2 é uma ferramenta de desenvolvimento que roda em um <frame>; a
                    // proteção contra clickjacking (X-Frame-Options) só pode ficar desabilitada
                    // fora do perfil de produção, onde o console nem sequer é exposto.
                    if (!producao) {
                        headers.frameOptions(frame -> frame.disable());
                    }
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(ROTAS_PUBLICAS).permitAll();
                    if (!producao) {
                        auth.requestMatchers(ROTA_H2_CONSOLE).permitAll();
                    }
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
