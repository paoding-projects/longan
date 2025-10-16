package dev.paoding.longan.core;

import dev.paoding.longan.channel.dubbo.DubboFilter;
import dev.paoding.longan.channel.http.HttpServer;
import dev.paoding.longan.channel.http.MethodInvocationProvider;
import dev.paoding.longan.channel.http.WebSocketListener;
import dev.paoding.longan.channel.http.WebSocketListenerHandler;
import dev.paoding.longan.doc.DocumentService;
import dev.paoding.longan.util.StringUtils;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.ServiceConfig;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.List;

import static dev.paoding.longan.util.TypeUtils.getModelClass;

@Component
public class ServiceInitializingBean implements BeanFactoryAware, InitializingBean, SmartInitializingSingleton {
    @Resource
    private WebSocketListenerHandler webSocketListenerHandler;
    @Resource
    private MethodInvocationProvider methodInvocationProvider;
    @Resource
    private ApplicationContext context;
    @Resource
    private HttpServer httpServer;
    @Value("${longan.api.enable:false}")
    private boolean apiEnabled;
    private BeanFactory beanFactory;


    @Override
    public void setBeanFactory(@NonNull BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }

    @Override
    public void afterPropertiesSet() {
        registerWebSocketListener();
        exportDubboService();
        exportHttpService();
        exportApiDocument();
        startupHttpServer();
    }

    @Override
    public void afterSingletonsInstantiated() {
    }

    private void startupHttpServer() {
        try {
            httpServer.startup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private void registerWebSocketListener() {
        String[] candidateNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors((ListableBeanFactory) beanFactory, WebSocketListener.class);
        for (String candidateName : candidateNames) {
            Class<?> modelClass = getModelClass(AopUtils.getTargetClass(beanFactory.getBean(candidateName)));
            webSocketListenerHandler.addWebSocketListener(StringUtils.lowerFirst(modelClass.getSimpleName()),(WebSocketListener) beanFactory.getBean(candidateName));
        }
    }

    private void exportDubboService() {
        if (context.containsBean("dubboInterceptor")) {
            List<MethodInvocation> methodInvocations = methodInvocationProvider.getDubboMethodInvocations();
            for (MethodInvocation methodInvocation : methodInvocations) {
                ServiceConfig<Object> serviceConfig = new ServiceConfig<>();
                serviceConfig.setInterface(methodInvocation.getServiceInterface());
                serviceConfig.setRef(context.getBean(methodInvocation.getServiceClass()));
                serviceConfig.setFilter(DubboFilter.class.getName());
                serviceConfig.setRetries(0);
                serviceConfig.export();
            }
        }
    }

    private void exportHttpService() {
        List<MethodInvocation> methodInvocations = methodInvocationProvider.getHttpMethodInvocations();
        for (MethodInvocation methodInvocation : methodInvocations) {
            methodInvocation.setService(context.getBean(methodInvocation.getServiceClass()));
        }
    }

    private void exportApiDocument() {
        if (apiEnabled) {
            DocumentService.check();
        }
    }

}
