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
        String base = SCHEMA + "." + quoteIdent(table);
        String orderBy = resolveOrderBy(columns);

        List<Object> countParams = new ArrayList<>(params);
        long total = jdbc.queryForObject("SELECT COUNT(*) FROM " + base + whereClause, Long.class, countParams.toArray());

        List<Object> rowParams = new ArrayList<>(params);
        rowParams.add(size);
        rowParams.add((long) page * size);

        List<List<String>> rows = jdbc.query(
                "SELECT * FROM " + base + whereClause + orderBy + " LIMIT ? OFFSET ?",
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

    /**
     * Tables like processed_events / flyway_schema_history have no created_at.
     * Pick a stable DESC key from columns that actually exist.
     */
    private static String resolveOrderBy(List<String> columns) {
        for (String candidate : List.of(
                "created_at", "updated_at", "processed_at", "installed_on",
                "installed_rank", "event_id", "id")) {
            if (columns.stream().anyMatch(c -> c.equalsIgnoreCase(candidate))) {
                return " ORDER BY " + quoteIdent(candidate) + " DESC";
            }
        }
        return "";
    }

    private static String quoteIdent(String ident) {
        if (ident == null || !ident.matches("[A-Za-z_][A-Za-z0-9_]*")) {
            throw new IllegalArgumentException("Invalid identifier");
        }
        return "\"" + ident + "\"";
    }

    private void appendAnd(StringBuilder sb) {
        if (sb.length() > 0) sb.append(" AND ");
    }
}
