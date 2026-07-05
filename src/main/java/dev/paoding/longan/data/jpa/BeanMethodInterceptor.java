package dev.paoding.longan.data.jpa;

import dev.paoding.longan.service.SystemException;
import dev.paoding.longan.util.BeanProxyUtils;
import dev.paoding.longan.util.EntityUtils;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;
import org.springframework.dao.EmptyResultDataAccessException;

import java.lang.reflect.Method;
import java.util.Map;

public class BeanMethodInterceptor<T> implements MethodInterceptor, BeanProxy {
    private T bean;
    private final Object id;
    private final Class<?> type;
    private final MetaTable<T> metaTable;
    private final JdbcSession jdbcSession;

    public BeanMethodInterceptor(JdbcSession jdbcSession, Class<T> type, Object id) {
        this.id = id;
        this.type = type;
        this.jdbcSession = jdbcSession;
        this.metaTable = MetaTableFactory.get(type);
    }

    @Override
    public Object intercept(Object object, Method method, Object[] args, MethodProxy methodProxy) throws Throwable {
        if (BeanProxyUtils.GET_ID_METHOD.equals(method.getName())) {
            return id;
        }
        if (BeanProxyUtils.GET_ORIGINAL_METHOD.equals(method)) {
            return getOriginal();
        }
        if (BeanProxyUtils.GET_TYPE_METHOD.equals(method)) {
            return getType();
        }
        if (bean == null) {
            load();
        }
        return method.invoke(bean, args);
    }

    @Override
    public Object getOriginal() {
        if (bean == null) {
            load();
        }
        return bean;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    private void load() {
        try {
            this.bean = jdbcSession.queryForObject(metaTable.selectByPrimaryKey(), Map.of("id", id), metaTable.getRowMapper());
            EntityUtils.wrap(metaTable, this.bean);
        } catch (EmptyResultDataAccessException e) {
            throw new SystemException(type.getSimpleName() + " with id " + id + " not found.");
        }
    }
}
