package com.nyberg.notifications.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin/db")
public class DbViewerController {

    private static final String SCHEMA = "notifications";

    private final JdbcTemplate jdbc;

    public DbViewerController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/tables")
    public List<String> tables() {
        return jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = ? ORDER BY table_name",
                String.class, SCHEMA);
    }

    @GetMapping("/tables/{table}")
    public Map<String, Object> tableData(
            @PathVariable String table,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size,
            // notification-specific filters — ignored for other tables
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String channel,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String organizationId,
            @RequestParam(required = false) String tenantId,
            @RequestParam(required = false) String dateFrom,
            @RequestParam(required = false) String dateTo) {

        boolean valid = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_name = ?",
                String.class, SCHEMA, table).size() == 1;

        if (!valid) throw new IllegalArgumentException("Unknown table: " + table);

        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
                String.class, SCHEMA, table);

        // Build WHERE clause — only applied when viewing the notifications table
        StringBuilder where = new StringBuilder();
        List<Object> params = new ArrayList<>();

        if ("notifications".equals(table)) {
            if (status   != null && !status.isBlank())   { appendAnd(where); where.append("status = ?");                params.add(status.trim()); }
            if (channel  != null && !channel.isBlank())  { appendAnd(where); where.append("channel = ?");               params.add(channel.trim()); }
            if (priority != null && !priority.isBlank()) { appendAnd(where); where.append("priority = ?");              params.add(priority.trim()); }
            if (source   != null && !source.isBlank())   { appendAnd(where); where.append("source ILIKE ?");            params.add("%" + source.trim() + "%"); }
            if (organizationId != null && !organizationId.isBlank()) {
                appendAnd(where);
                where.append("organization_id = ?::uuid");
                params.add(organizationId.trim());
            }
            if (tenantId != null && !tenantId.isBlank()) { appendAnd(where); where.append("tenant_id = ?::uuid");       params.add(tenantId.trim()); }
            if (dateFrom != null && !dateFrom.isBlank()) { appendAnd(where); where.append("created_at >= ?::timestamptz"); params.add(dateFrom.trim()); }
            if (dateTo   != null && !dateTo.isBlank())   { appendAnd(where); where.append("created_at <= ?::timestamptz"); params.add(dateTo.trim()); }
        }

        String whereClause = where.length() > 0 ? " WHERE " + where : "";
        String base = SCHEMA + "." + table;

        List<Object> countParams = new ArrayList<>(params);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + base + whereClause, Long.class, countParams.toArray());

        List<Object> rowParams = new ArrayList<>(params);
        rowParams.add(size);
        rowParams.add((long) page * size);

        List<List<String>> rows = jdbc.query(
                "SELECT * FROM " + base + whereClause + " ORDER BY created_at DESC LIMIT ? OFFSET ?",
                rs -> {
                    List<List<String>> result = new ArrayList<>();
                    while (rs.next()) {
                        List<String> row = new ArrayList<>();
                        for (int i = 1; i <= columns.size(); i++) {
                            Object val = rs.getObject(i);
                            row.add(val == null ? null : val.toString());
                        }
                        result.add(row);
                    }
                    return result;
                },
                rowParams.toArray());

        return Map.of(
                "columns", columns,
                "rows", rows,
                "total", total,
                "page", page,
                "size", size);
    }

    private void appendAnd(StringBuilder sb) {
        if (sb.length() > 0) sb.append(" AND ");
    }
}
