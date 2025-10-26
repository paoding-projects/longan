package dev.paoding.longan.data.jpa;

import org.springframework.cglib.beans.BeanMap;

import java.util.List;
import java.util.Map;

public class SqlSession {
    private final JdbcSession jdbcSession;

    public SqlSession(JdbcSession jdbcSession) {
        this.jdbcSession = jdbcSession;
    }

    public int delete(Object entity) {
        Object id = BeanMap.create(entity).get("id");
        return deleteById(entity.getClass(), id);
    }

    public int deleteById(Class<?> type, Object id) {
        MetaTable<?> masterMetaTable = MetaTableFactory.get(type);
        return jdbcSession.update(masterMetaTable.deleteByPrimaryKey(), new Object[]{id});
    }

    public int deleteAll(Class<?> type) {
        MetaTable<?> metaTable = MetaTableFactory.get(type);
        String sql = "delete from " + metaTable.getName();
        return jdbcSession.update(sql);
    }

    public int deleteById(Class<?> type, List<?> idList) {
        MetaTable<?> metaTable = MetaTableFactory.get(type);
        Map<String, Object> paramMap = Map.of("idList", idList);
        String sql = "delete from " + metaTable.getName() + " where id in (:idList)";
        return jdbcSession.update(sql, paramMap);
    }
}
