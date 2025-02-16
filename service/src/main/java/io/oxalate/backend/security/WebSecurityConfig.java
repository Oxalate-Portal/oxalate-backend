package io.oxalate.backend.security;

import static io.oxalate.backend.api.UrlConstants.API;
import static io.oxalate.backend.api.UrlConstants.DIVE_PLANS_URL;
import static io.oxalate.backend.api.UrlConstants.DOCUMENTS_URL;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import static io.oxalate.backend.api.UrlConstants.PAGES_URL;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.security.jwt.AuthEntryPointJwt;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsServiceImpl;
import io.oxalate.backend.service.RecaptchaService;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import static org.springframework.security.config.Customizer.withDefaults;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final RecaptchaService recaptchaService;
    private final AppEventPublisher appEventPublisher;
    private final JwtUtils jwtUtils;

    @Value("${oxalate.cors.allowed-origins}")
    private String allowedOrigins;
    @Value("${oxalate.cors.max-age}")
    private long maxAge;
    @Value("${oxalate.cors.cors-pattern}")
    private String corsPattern;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // This is not working, so we have to disable CSRF for now
        // https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html#_i_am_using_angularjs_or_another_javascript_framework
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers(API + "/auth/**")
                            .permitAll()
                            .requestMatchers(PAGES_URL + "/**") // We check the permissions in the calls as some pages may not require authentication
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, FILES_URL + "/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, DOCUMENTS_URL + "/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, DIVE_PLANS_URL + "/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, "/api/configurations/frontend") // Allow fetching of frontend configurations
                            .permitAll()
                            .requestMatchers("/actuator/**")
                            .permitAll()
                            .requestMatchers("/v3/api-docs/**")
                            .permitAll()
                            .requestMatchers(API + "/test/**") // This is only available in development environment
                            .permitAll()
                            .anyRequest()
                            .authenticated();
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(new RecaptchaFilter(recaptchaService, appEventPublisher), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtCookieAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }

    @Bean(name = "corsConfigurationSource")
    public CorsConfigurationSource corsConfigurationSource() {
        log.debug("Configuring CORS with allowed origins {}, maxAge {} and corsPattern {}", allowedOrigins, maxAge, corsPattern);
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Collections.singletonList(allowedOrigins));
        configuration.setAllowedMethods(Collections.singletonList("*"));
        configuration.setAllowedHeaders(Collections.singletonList("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(maxAge);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(corsPattern, configuration);
        return source;
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfiguration) throws Exception {
        return authConfiguration.getAuthenticationManager();
    }

    @Bean
    public JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter(JwtUtils jwtUtils) {
        return new JwtCookieAuthenticationFilter(jwtUtils, userDetailsService);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
