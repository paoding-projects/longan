package dev.paoding.longan.data;

import dev.paoding.longan.data.jpa.Database;
import dev.paoding.longan.data.jpa.SqlParser;
import dev.paoding.longan.service.ConstraintViolationException;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

import java.util.regex.Pattern;

@Getter
@Setter
@FieldNameConstants
@Entity(alias = "分页对象", virtual = true)
public class Pageable {
    private static final Pattern PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-.]+$");

    /**
     * 第几页，从1开始，默认为第1页
     */
    private int page;
    /**
     * 每一页的大小，默认为20
     */
    private int size;
    /**
     * 排序属性，默认为 id
     */
    private String sort;
    /**
     * 是否倒序排列，默认为 true
     */
    private boolean desc;

    public Pageable() {
    }

    public Pageable(int page) {
        this(page, 20, "id", true);
    }

    public Pageable(int page, int size) {
        this(page, size, "id", true);
    }

    public Pageable(int page, int size, String sort) {
        this(page, size, sort, true);
    }

    public Pageable(int page, int size, String sort, boolean desc) {
        this.page = page;
        this.size = size;
        this.sort = sort;
        this.desc = desc;
    }

    private int offset() {
        return (page - 1) * size;
    }

    private int limit() {
        return size;
    }

    public String toSql(String databaseType) {
        StringBuilder sb = new StringBuilder();
        if (sort != null && !sort.isBlank()) {
            if (!PATTERN.matcher(sort).matches()) {
                String message = "Unsupported sort field name";
                throw new ConstraintViolationException("pageable.sort.unsupported", message);
            }
            sb.append(" ORDER BY ").append(SqlParser.toColumnName(sort));
            sb.append(desc ? " DESC" : " ASC");
        }

        if (Database.POSTGRESQL.equals(databaseType)) {
            sb.append(" OFFSET ").append(offset()).append(" LIMIT ").append(limit());
        } else if (Database.MYSQL.equals(databaseType)) {
            sb.append(" LIMIT ").append(offset()).append(", ").append(limit());
        }
        return sb.toString();
    }

}
