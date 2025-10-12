package dev.paoding.longan.core;

import dev.paoding.longan.data.jpa.JpaAutoRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import({RpcServiceAutoRegistrar.class, JdbcAutoConfiguration.class, JpaAutoRegistrar.class})
public class LonganConfiguration {

}
