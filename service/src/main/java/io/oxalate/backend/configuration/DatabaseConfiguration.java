package io.oxalate.backend.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@EnableJpaRepositories(entityManagerFactoryRef = "oxalateEntityManagerFactory",
        transactionManagerRef = "oxalateTransactionManager",
        basePackages = "io.oxalate.backend.repository")
public class DatabaseConfiguration {
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties oxalateDataSourceProperties() {
        return new DataSourceProperties();
    }
}
