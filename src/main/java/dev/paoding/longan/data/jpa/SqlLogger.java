package dev.paoding.longan.data.jpa;

import dev.paoding.longan.util.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.HashMap;
import java.util.Map;

public class SqlLogger {
    private static final Logger logger = LoggerFactory.getLogger(SqlLogger.class);
    private static boolean enable;

    public static void init(boolean enable) {
        SqlLogger.enable = enable;
    }

    public static void log(String sql) {
        if (enable) {
            logger.info("statement\n{}", sql);
        }
    }

    public static void log(String databaseName, String sql) {
        if (enable) {
            logger.info("{} - {}", databaseName, sql);
        }
    }

    public static void log(Map<String, ?> paramMap) {
        if (enable) {
            logger.info("parameter {}", JsonUtils.toJson(paramMap));
        }
    }

    public static void log(SqlParameterSource paramSource) {
        if (enable) {
            Map<String, Object> paramMap = new HashMap<>();
            for (String name : paramSource.getParameterNames()) {
                paramMap.put(name, paramSource.getValue(name));
            }
            log(paramMap);
        }
    }


}
