package com.wcf.wcfaiagent.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * 默认数据源（PostgreSQL，供 PgVector 向量知识库使用）的显式声明。
 *
 * <p>为什么要显式声明：项目引入了第二个数据源（MybatisConfig 中的 MySQL 连接池）后，
 * Spring Boot 的 DataSourceAutoConfiguration 因为 @ConditionalOnMissingBean(DataSource)
 * 条件不再成立而自动退避，默认数据源就不会被创建了。这里手动复刻自动装配原本会做的事：
 * 绑定 spring.datasource 前缀配置并创建 HikariCP 连接池，同时标注 @Primary，
 * 让 JdbcTemplate、PgVector 等按类型注入的地方默认拿到本数据源。
 */
@Configuration
public class DataSourceConfig {

    /** 读取 spring.datasource 前缀配置（url/username/password/driver-class-name） */
    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    /** PostgreSQL 连接池（HikariCP），@Primary：PgVector / JdbcTemplate 默认使用 */
    @Bean
    @Primary
    public HikariDataSource dataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }
}
