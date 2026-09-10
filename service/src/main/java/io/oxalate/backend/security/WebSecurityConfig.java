package io.oxalate.backend.security;

import static io.oxalate.backend.api.UrlConstants.API;
import static io.oxalate.backend.api.UrlConstants.DIVE_PLANS_URL;
import static io.oxalate.backend.api.UrlConstants.DOCUMENTS_URL;
import static io.oxalate.backend.api.UrlConstants.FILES_URL;
import static io.oxalate.backend.api.UrlConstants.PAGES_URL;
import io.oxalate.backend.events.AppEventPublisher;
import io.oxalate.backend.security.jwt.JwtUtils;
import io.oxalate.backend.security.service.UserDetailsServiceImpl;
import io.oxalate.backend.service.RecaptchaService;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    /**
     * OWASP A04:2025 - BCrypt work factor. Raised from the Spring default of 10.
     */
    public static final int BCRYPT_STRENGTH = 12;

    private final UserDetailsServiceImpl userDetailsService;
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
        // OWASP A01:2025 - CSRF token protection cannot be used because the SPA authenticates with an
        // HttpOnly JWT cookie and therefore cannot echo a token. The equivalent protection is provided by
        // SameSite=Strict on the cookie (enforced at startup by SecurityPreflight) plus the
        // CsrfOriginValidationFilter below, which rejects cross-origin state-changing requests.
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(withDefaults())
                .authorizeHttpRequests(auth -> {
                    auth
                            .requestMatchers(API + "/auth/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, API + "/third-party/*")
                            .permitAll()
                            .requestMatchers(PAGES_URL + "/**") // We check the permissions in the calls as some pages may not require authentication
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, FILES_URL + "/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, DOCUMENTS_URL + "/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, DIVE_PLANS_URL + "/**")
                            .permitAll()
                            .requestMatchers(HttpMethod.GET, API + "/configurations/frontend") // Allow fetching of frontend configurations
                            .permitAll()
                            // OWASP A02:2025 - only the endpoints that are actually exposed are public. Everything
                            // else below /actuator (env, beans, heapdump, loggers, ...) requires an administrator,
                            // so enabling a new management endpoint cannot silently expose it.
                            .requestMatchers("/actuator/health", "/actuator/info")
                            .permitAll()
                            .requestMatchers("/actuator/openapi/**", "/actuator/swagger-ui/**", "/actuator/swagger-ui.html")
                            .permitAll()
                            .requestMatchers("/actuator/**")
                            .hasRole("ADMIN")
                            .requestMatchers("/v3/api-docs/**")
                            .permitAll()
                            .requestMatchers(API + "/test/**") // This is only available in development environment
                            .permitAll()
                            .anyRequest()
                            .authenticated();
                })
                // OWASP A02:2025 - send hardening directives on every response. The API returns JSON only,
                // so the policy can be maximally restrictive.
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(
                                "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'"))
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
                        .httpStrictTransportSecurity(hsts -> hsts.includeSubDomains(true)
                                                                 .maxAgeInSeconds(31_536_000))
                        .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                        .permissionsPolicyHeader(permissions -> permissions.policy("camera=(), microphone=(), geolocation=(), payment=()")))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(new CsrfOriginValidationFilter(getAllowedOrigins()), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new RecaptchaFilter(recaptchaService, appEventPublisher), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtCookieAuthenticationFilter(jwtUtils), UsernamePasswordAuthenticationFilter.class)
        ;

        return http.build();
    }

    /**
     * Parses {@code oxalate.cors.allowed-origins} into individual origins.
     * <p>
     * The property is a comma-separated list; passing the raw string as a single allowed origin means no
     * browser origin ever matches, which silently breaks both CORS and the origin-based CSRF defence.
     *
     * @return the configured origins, never empty when the property is set
     */
    Set<String> getAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                     .map(String::trim)
                     .filter(origin -> !origin.isEmpty())
                     .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Bean(name = "corsConfigurationSource")
    public CorsConfigurationSource corsConfigurationSource() {
        var origins = getAllowedOrigins();
        log.debug("Configuring CORS with allowed origins {}, maxAge {} and corsPattern {}", origins, maxAge, corsPattern);
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.copyOf(origins));
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
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
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

    /**
     * OWASP A04:2025 (CWE-916). BCrypt with an explicit work factor of 12 rather than the library
     * default of 10. Existing hashes keep working because the cost is encoded in the stored hash;
     * only newly written hashes use the higher factor.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
