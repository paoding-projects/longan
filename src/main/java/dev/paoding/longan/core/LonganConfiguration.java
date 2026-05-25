package dev.paoding.longan.core;

import dev.paoding.longan.data.jpa.JpaAutoRegistrar;
import dev.paoding.longan.data.jpa.ShortValueEnumConverterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@PropertySource(value = "classpath:application.yml", factory = YamlPropertySourceFactory.class)
@EnableScheduling
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Import({RpcServiceAutoRegistrar.class, JdbcAutoConfiguration.class, JpaAutoRegistrar.class})
public class LonganConfiguration {

    @Bean
    public ConversionService conversionService() {
        DefaultConversionService conversionService = new DefaultConversionService();
        conversionService.addConverterFactory(new ShortValueEnumConverterFactory());
        return conversionService;
    }
}
