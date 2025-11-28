package dev.paoding.longan.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.zaxxer.hikari.HikariDataSource;
import dev.paoding.longan.data.jpa.*;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.lang.NonNull;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

public class JdbcAutoConfiguration implements ImportBeanDefinitionRegistrar, EnvironmentAware {
    private Environment environment;

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) registry;
        HashMap<String, Map<String, String>> datasourceConfig = getStringMapHashMap();
        datasourceConfig.forEach((name, config) -> {
            DataSource dataSource = create(config);
            String databaseType = getDatabaseType(config.get("url"));
            defaultListableBeanFactory.registerSingleton(name + "DataSource", dataSource);
            defaultListableBeanFactory.registerSingleton(name + "JdbcSession", jdbcSession(databaseType, dataSource));

            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(DataSourceTransactionManager.class);
            beanDefinition.getConstructorArgumentValues().addGenericArgumentValue(dataSource);
            if (name.equals("default")) {
                beanDefinition.setPrimary(true);
            }
            defaultListableBeanFactory.registerBeanDefinition(name + "TxManager", beanDefinition);
        });
    }

    @Override
    public void setEnvironment(@NonNull Environment environment) {
        this.environment = environment;
        boolean showSql = environment.getProperty("longan.show-sql", Boolean.class, false);
        SqlLogger.init(showSql);
    }

    private String getDatabaseType(String url) {
        if (url.contains(Database.POSTGRESQL)) {
            return Database.POSTGRESQL;
        } else if (url.contains(Database.MYSQL)) {
            return Database.MYSQL;
        } else if (url.contains(Database.ORACLE)) {
            return Database.ORACLE;
        }
        throw new RuntimeException("unsupport database type");
    }

    private PlatformTransactionManager txManager(DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    private JdbcSession jdbcSession(String databaseType, DataSource dataSource) {
        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        JdbcSession jdbcSession = new JdbcSession(databaseType, namedParameterJdbcTemplate);
        BeanFactory.register(jdbcSession);
        return jdbcSession;
    }

//    private DataSource dataSource() {
//        boolean showSql = environment.getProperty("longan.datasource.show-sql", Boolean.class, false);
//        SqlLogger.init(showSql);
//        Database.init(environment.getProperty("longan.datasource.url"), environment.getProperty("longan.datasource.username"), environment.getProperty("longan.datasource.password"));
//        return  null;
//    }

    private HashMap<String, Map<String, String>> getStringMapHashMap() {
        String prefix = "longan.datasource.";
        ConfigurableEnvironment env = (ConfigurableEnvironment) environment;
        HashMap<String, Map<String, String>> datasourceConfig = new HashMap<>();
        for (PropertySource<?> ps : env.getPropertySources()) {
            if (ps instanceof org.springframework.core.env.MapPropertySource) {
                Map<String, Object> source = ((org.springframework.core.env.MapPropertySource) ps).getSource();
                source.forEach((k, v) -> {
                    if (k.startsWith(prefix)) {
                        String[] array = k.split("\\.");
                        String name = array[2];
                        if (!datasourceConfig.containsKey(name)) {
                            datasourceConfig.put(name, new HashMap<>());
                        }
                        datasourceConfig.get(name).put(array[3], v.toString());
                    }
                });
            }
        }
        return datasourceConfig;
    }

    private DataSource create(Map<String, String> config) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setThreadFactory(new ThreadFactoryBuilder().setNameFormat("hikari-thread-%d").build());
        hikariDataSource.setMinimumIdle(Integer.parseInt(config.getOrDefault("idle-min", "10")));
        hikariDataSource.setMaximumPoolSize(Integer.parseInt(config.getOrDefault("longan.datasource.pool-max", "100")));
        hikariDataSource.setJdbcUrl(config.get("url"));
        hikariDataSource.setUsername(config.get("username"));
        hikariDataSource.setPassword(config.get("password"));
        hikariDataSource.addDataSourceProperty("cachePrepStmts", true);
        hikariDataSource.addDataSourceProperty("prepStmtCacheSize", 250);
        hikariDataSource.addDataSourceProperty("prepStmtCacheSqlLimit", 2048);
        hikariDataSource.addDataSourceProperty("useServerPrepStmts", true);
        return hikariDataSource;
    }
}
