package dev.paoding.longan.data.jpa;

import com.github.vertical_blank.sqlformatter.core.AbstractFormatter;
import com.github.vertical_blank.sqlformatter.core.FormatConfig;
import dev.paoding.longan.util.GsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.util.HashMap;
import java.util.Map;

public class SqlLogger {
    private static final Logger logger = LoggerFactory.getLogger(SqlLogger.class);
    private static boolean enable;
    private static boolean pretty;
    private static AbstractFormatter formatter;

    public static void init(boolean enable, boolean pretty) {
        SqlLogger.enable = enable;
        SqlLogger.pretty = pretty;
        if (pretty) {
            FormatConfig formatConfig = FormatConfig.builder()
                    .indent("    ")
                    .uppercase(true)
                    .linesBetweenQueries(2)
                    .maxColumnLength(100).build();
            formatter = new StandardSqlFormatter(formatConfig);
        }
    }

    public static void log(String sql) {
        if (enable) {
            if (pretty) {
                sql = formatter.format(sql);
            }
            logger.info("statement\n{}", sql);
        }
    }

    public static void log(Map<String, ?> paramMap) {
        if (enable) {
            logger.info("parameter\n{}", GsonUtils.toJson(paramMap));
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
