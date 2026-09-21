package io.oxalate.backend.configuration;

import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registers the custom controller argument resolvers. Custom resolvers run before Spring's catch-all
 * {@code @ModelAttribute} binding, so a plain {@code PagedRequest} parameter is bound by
 * {@link PagedRequestArgumentResolver}.
 */
@Configuration
public class WebMvcConfiguration implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new PagedRequestArgumentResolver());
    }
}
