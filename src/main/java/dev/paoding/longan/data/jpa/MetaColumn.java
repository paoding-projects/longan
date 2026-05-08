package dev.paoding.longan.data.jpa;

import dev.paoding.longan.service.SystemException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class MetaColumn {
    private String tableName;
    private String name;
    private String alias;
    private String comment;
    private int length = 64;
    private boolean nullable;
    private boolean insertable;
    private boolean updatable;
    private boolean unique;
    private boolean primaryKey;
    private int scale;
    private int precision;
    private Generator generator;
    private Class<?> type;
    private Field field;

    public Generator getGenerator() {
        return generator;
    }

    public void setGenerator(Generator generator) {
        this.generator = generator;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public Field getField() {
        return field;
    }

    public void setField(Field field) {
        this.field = field;
        this.field.setAccessible(true);
    }

    public Class<?> getType() {
        return type;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Object getValue(Object object) {
        try {
            return this.field.get(object);
        } catch (IllegalAccessException e) {
            throw new SystemException(e);
        }
    }

    public void setType(Class type) {
        this.type = type;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = SqlParser.toDatabaseName(name);
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isUnique() {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    public boolean isPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(boolean primaryKey) {
        this.primaryKey = primaryKey;
    }

    public int getScale() {
        return scale;
    }

    public void setScale(int scale) {
        this.scale = scale;
    }

    public int getPrecision() {
        return precision;
    }

    public void setPrecision(int precision) {
        this.precision = precision;
    }

    public boolean isUpdatable() {
        return updatable;
    }

    public void setUpdatable(boolean updatable) {
        this.updatable = updatable;
    }

    public boolean isInsertable() {
        return insertable;
    }

    public void setInsertable(boolean insertable) {
        this.insertable = insertable;
    }


    public String generateColumnStatement(String databaseType) {
        if (Database.POSTGRESQL.equals(databaseType)) {
            return generatePostgresqlText();
        } else if (Database.MYSQL.equals(databaseType)) {
            return generateMySqlText();
        }
        throw new RuntimeException("not support database " + databaseType);
    }

    public String generateColumnCommentStatement(String databaseType) {
        if (Database.POSTGRESQL.equals(databaseType)) {
            if (comment != null && !comment.isBlank()) {
                return "COMMENT ON COLUMN " + tableName + "." + name + " IS '" + comment + "'";
            }
        }
        return null;
    }

    private String generateMySqlText() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
            if (primaryKey && generator.equals(Generator.AUTO)) {
                sb.append(" INT AUTO_INCREMENT");
            } else {
                sb.append(" INT");
            }
        } else if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
            if (primaryKey && generator.equals(Generator.AUTO)) {
                sb.append(" BIGINT AUTO_INCREMENT");
            } else {
                sb.append(" BIGINT");
            }
        } else if (Short.class.isAssignableFrom(type) || short.class.isAssignableFrom(type)) {
            if (primaryKey && generator.equals(Generator.AUTO)) {
                sb.append(" SMALLINT AUTO_INCREMENT");
            } else {
                sb.append(" SMALLINT");
            }
        } else if (Byte.class.isAssignableFrom(type) || byte.class.isAssignableFrom(type)) {
            if (primaryKey && generator.equals(Generator.AUTO)) {
                sb.append(" TINYINT AUTO_INCREMENT");
            } else {
                sb.append(" TINYINT");
            }
        } else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
            if (getPrecision() > 0 || getScale() > 0) {
                sb.append(" NUMERIC(" + getPrecision() + ", " + getScale() + ")");
            } else {
                sb.append(" REAL");
            }
        } else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
            if (getPrecision() > 0 || getScale() > 0) {
                sb.append(" NUMERIC(" + getPrecision() + ", " + getScale() + ")");
            } else {
                sb.append(" DOUBLE PRECISION");
            }
        } else if (BigDecimal.class.isAssignableFrom(type)) {
            if (getPrecision() > 0 || getScale() > 0) {
                sb.append(" DECIMAL(" + getPrecision() + ", " + getScale() + ")");
            } else {
                sb.append(" DECIMAL PRECISION");
            }
        } else if (String.class.isAssignableFrom(type)) {
            if (length > 0) {
                sb.append(" NVARCHAR(" + length + ")");
            } else {
                sb.append(" TEXT");
            }
        } else if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
            sb.append(" BOOLEAN");
        } else if (LocalDateTime.class.isAssignableFrom(getType())) {
            sb.append(" DATETIME");
        } else if (LocalDate.class.isAssignableFrom(getType())) {
            sb.append(" DATE");
        } else if (LocalTime.class.isAssignableFrom(getType())) {
            sb.append(" TIME");
        } else if (Instant.class.isAssignableFrom(getType())) {
            sb.append(" DATETIME");
        } else if (Enum.class.isAssignableFrom(getType())) {
            sb.append(" NVARCHAR(" + length + ")");
        } else {
            throw new RuntimeException("not support" + getType());
        }

        if (isPrimaryKey()) {
            sb.append(" PRIMARY KEY ");
        } else if (!isNullable()) {
            sb.append(" NOT NULL");
        }

        if (comment != null && !comment.isBlank()) {
            sb.append(" COMMENT '").append(comment).append("'");
        }

        return sb.toString();
    }

    private String generatePostgresqlText() {
        StringBuilder sb = new StringBuilder();
        sb.append(name);
        if (Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
            if (primaryKey && generator.equals(Generator.AUTO)) {
                sb.append(" SERIAL");
            } else {
                sb.append(" INT");
            }
        } else if (Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
            if (primaryKey && generator.equals(Generator.AUTO)) {
                sb.append(" BIGSERIAL");
            } else {
                sb.append(" BIGINT");
            }
        } else if (Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
            if (getPrecision() > 0 || getScale() > 0) {
                sb.append(" NUMERIC(" + getPrecision() + "," + getScale() + ")");
            } else {
                sb.append(" REAL");
            }
        } else if (Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
            if (getPrecision() > 0 || getScale() > 0) {
                sb.append(" NUMERIC(" + getPrecision() + "," + getScale() + ")");
            } else {
                sb.append(" DOUBLE PRECISION");
            }
        } else if (BigDecimal.class.isAssignableFrom(type)) {
            if (getPrecision() > 0 || getScale() > 0) {
                sb.append(" DECIMAL(" + getPrecision() + ", " + getScale() + ")");
            } else {
                sb.append(" DECIMAL PRECISION");
            }
        } else if (Short.class.isAssignableFrom(type) || short.class.isAssignableFrom(type)) {
            sb.append(" SMALLINT");
        } else if (String.class.isAssignableFrom(type)) {
            sb.append(" TEXT");
        } else if (Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
            sb.append(" BOOLEAN");
        } else if (LocalDateTime.class.isAssignableFrom(getType())) {
            sb.append(" TIMESTAMP WITHOUT TIME ZONE");
        } else if (LocalDate.class.isAssignableFrom(getType())) {
            sb.append(" DATE");
        } else if (LocalTime.class.isAssignableFrom(getType())) {
            sb.append(" TIME WITHOUT TIME ZONE");
        } else if (Instant.class.isAssignableFrom(getType())) {
            sb.append(" TIMESTAMP WITH TIME ZONE");
        } else if (Enum.class.isAssignableFrom(getType())) {
            sb.append(" TEXT");
        } else if (type.isArray()) {
            if (String.class.isAssignableFrom(type.getComponentType())) {
                sb.append(" TEXT[]");
            }
        } else {
            throw new RuntimeException("not support" + getType());
        }

        if (isPrimaryKey()) {
            sb.append(" CONSTRAINT pk_" + tableName + " PRIMARY KEY");
        } else if (!isNullable()) {
            sb.append(" NOT NULL");
        }

        return sb.toString();
    }
}
