package com.wcf.wcfaiagent.config;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import javax.sql.DataSource;

/**
 * 智能体会话历史（本地 MySQL）独立数据源 + MyBatis 配置。
 *
 * <p>注意：本项目还有另一个数据源——application-local.yml 中 spring.datasource 指向的
 * PostgreSQL，供 PgVector 向量知识库（rag 包）使用。两者相互独立：
 * <ul>
 *   <li>PostgreSQL（spring.datasource）：PgVector 知识库，由 Spring Boot 自动装配；</li>
 *   <li>MySQL（mysql.datasource 前缀）：会话历史持久化，由本配置类手工装配，
 *       连接池同样使用 HikariCP。</li>
 * </ul>
 * 全部 SQL 语句都写在 resources/mapper/*.xml 里，不落在 Java 代码中。
 * 设置 mysql.enabled=false 可整体关闭该数据源（后端启动不再依赖本地 MySQL）。
 */
@Configuration
@ConditionalOnProperty(name = "mysql.enabled", havingValue = "true", matchIfMissing = true)
@MapperScan(basePackages = "com.wcf.wcfaiagent.mapper", sqlSessionFactoryRef = "mysqlSqlSessionFactory")
public class MybatisConfig {

    /** 读取 mysql.datasource 前缀配置（url/username/password/driver-class-name） */
    @Bean
    @ConfigurationProperties("mysql.datasource")
    public DataSourceProperties mysqlDataSourceProperties() {
        return new DataSourceProperties();
    }

    /** MySQL 连接池（HikariCP）：经 DataSourceProperties 绑定，保证 url 正确映射到 jdbcUrl */
    @Bean
    public HikariDataSource mysqlDataSource(
            @Qualifier("mysqlDataSourceProperties") DataSourceProperties mysqlDataSourceProperties) {
        return mysqlDataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /** 应用启动时执行 classpath:sql/agent_chat_schema.sql，自动建库建表（IF NOT EXISTS，可重复执行） */
    @Bean
    public DataSourceInitializer mysqlSchemaInitializer(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        ResourceDatabasePopulator populator = new ResourceDatabasePopulator();
        populator.addScript(new ClassPathResource("sql/agent_chat_schema.sql"));
        populator.setSqlScriptEncoding("UTF-8");
        populator.setContinueOnError(false);
        DataSourceInitializer initializer = new DataSourceInitializer();
        initializer.setDataSource(mysqlDataSource);
        initializer.setDatabasePopulator(populator);
        return initializer;
    }

    /** MyBatis 核心工厂：绑定 MySQL 数据源，加载 mapper/*.xml，并开启驼峰命名映射 */
    @Bean
    public SqlSessionFactory mysqlSqlSessionFactory(@Qualifier("mysqlDataSource") DataSource mysqlDataSource)
            throws Exception {
        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(mysqlDataSource);
        // 实体类别名（XML 中可直接写 ChatSession / ChatMessage）
        factoryBean.setTypeAliasesPackage("com.wcf.wcfaiagent.entity");
        // Mapper XML 位置
        factoryBean.setMapperLocations(
                new PathMatchingResourcePatternResolver().getResources("classpath*:mapper/*.xml"));
        // 下划线列名 -> 驼峰属性（app_key -> appKey）
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factoryBean.setConfiguration(configuration);
        return factoryBean.getObject();
    }

    /** 事务管理器：绑定 MySQL 数据源，供批量保存消息、删除会话等操作使用 */
    @Bean
    public DataSourceTransactionManager mysqlTransactionManager(@Qualifier("mysqlDataSource") DataSource mysqlDataSource) {
        return new DataSourceTransactionManager(mysqlDataSource);
    }
}
