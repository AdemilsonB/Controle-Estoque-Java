package com.estoque.config;

import com.zaxxer.hikari.HikariDataSource;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = {"com.estoque.repository", "com.estoque.service", "com.estoque.security", "com.estoque.mapper"})
@EnableJpaRepositories(basePackages = "com.estoque.repository")
@EnableJpaAuditing
@EnableTransactionManagement
@PropertySource("classpath:app-${spring.profiles.active:h2}.properties")
public class PersistenceConfig {

    @Value("${db.url}") private String dbUrl;
    @Value("${db.username}") private String dbUsername;
    @Value("${db.password}") private String dbPassword;
    @Value("${db.driver}") private String dbDriver;
    @Value("${hibernate.dialect}") private String hibernateDialect;

    @Bean
    public DataSource dataSource() {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(dbUrl);
        ds.setUsername(dbUsername);
        ds.setPassword(dbPassword);
        ds.setDriverClassName(dbDriver);
        return ds;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
        emf.setDataSource(dataSource);
        emf.setPackagesToScan("com.estoque.entity");
        emf.setPersistenceProvider(new HibernatePersistenceProvider());

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        emf.setJpaVendorAdapter(vendorAdapter);

        Properties props = new Properties();
        props.setProperty("hibernate.dialect", hibernateDialect);
        props.setProperty("hibernate.hbm2ddl.auto", "validate");
        props.setProperty("hibernate.format_sql", "true");
        // Equivalente a spring.jpa.open-in-view: false no Boot: sem OpenEntityManagerInViewFilter
        // registrado no WebAppInitializer (Task 4), o comportamento já é o mesmo por padrão.
        emf.setJpaProperties(props);

        return emf;
    }

    @Bean
    public PlatformTransactionManager transactionManager(LocalContainerEntityManagerFactoryBean emf) {
        return new JpaTransactionManager(emf.getObject());
    }
}
