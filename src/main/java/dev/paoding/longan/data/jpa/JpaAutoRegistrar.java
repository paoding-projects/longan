package dev.paoding.longan.data.jpa;

import dev.paoding.longan.core.ClassPathBeanScanner;
import dev.paoding.longan.data.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.lang.NonNull;

import java.io.Serializable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpaAutoRegistrar implements ImportBeanDefinitionRegistrar {
    private final Logger logger = LoggerFactory.getLogger(JpaAutoRegistrar.class);

    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        logger.info("Register jpa repository");

        DefaultListableBeanFactory defaultListableBeanFactory = (DefaultListableBeanFactory) registry;

        Map<String, List<Class<?>>> map = new HashMap<>();

        List<Class<?>> repositoryClasses = ClassPathBeanScanner.getRepositoryClasses();
        for (Class<?> repositoryClass : repositoryClasses) {
            if (repositoryClass.isInterface()) {
                if (JpaRepository.class.isAssignableFrom(repositoryClass)) {
                    Type type = ((ParameterizedType) repositoryClass.getGenericInterfaces()[0]).getActualTypeArguments()[0];
                    Class<?> modelClass = (Class<?>) type;
                    if (modelClass.isAnnotationPresent(Entity.class) && !modelClass.getAnnotation(Entity.class).virtual()) {
                        String database = "default";
                        if (modelClass.isAnnotationPresent(Table.class)) {
                            database = modelClass.getAnnotation(Table.class).database();
                        }
                        if (!map.containsKey(database)) {
                            map.put(database, new ArrayList<>());
                        }
                        map.get(database).add(modelClass);


                        JdbcSession jdbcSession = defaultListableBeanFactory.getBean(database + "JdbcSession", JdbcSession.class);

                        JpaRepositoryProxy<?, ? extends Serializable> repositoryProxy = new JpaRepositoryProxy<>((Class<?>) type);
                        repositoryProxy.setJdbcSession(jdbcSession);

                        Object proxy = Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[]{repositoryClass}, repositoryProxy);
                        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(proxy.getClass());
                        builder.addConstructorArgValue(Proxy.getInvocationHandler(proxy));
                        AbstractBeanDefinition beanDefinition = builder.getBeanDefinition();
                        beanDefinition.setBeanClass(proxy.getClass());
                        registry.registerBeanDefinition(repositoryClass.getSimpleName(), beanDefinition);
                    }

                }
            }
        }

        map.forEach((database,entitylist)->{
            JdbcSession jdbcSession = defaultListableBeanFactory.getBean(database + "JdbcSession", JdbcSession.class);
            TableMetaDataManager tableMetaDataManager = TableMetaDataManager.create(jdbcSession);
            tableMetaDataManager.populate(entitylist);
        });
    }
}
