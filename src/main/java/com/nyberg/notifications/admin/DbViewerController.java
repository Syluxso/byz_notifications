package com.nyberg.notifications.admin;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

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
            @RequestParam(defaultValue = "100") int size) {

        boolean valid = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables " +
                "WHERE table_schema = ? AND table_name = ?",
                String.class, SCHEMA, table).size() == 1;

        if (!valid) throw new IllegalArgumentException("Unknown table: " + table);

        List<String> columns = jdbc.queryForList(
                "SELECT column_name FROM information_schema.columns " +
                "WHERE table_schema = ? AND table_name = ? ORDER BY ordinal_position",
                String.class, SCHEMA, table);

        long total = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + SCHEMA + "." + table, Long.class);

        List<List<String>> rows = jdbc.query(
                "SELECT * FROM " + SCHEMA + "." + table + " LIMIT ? OFFSET ?",
                rs -> {
                    List<List<String>> result = new java.util.ArrayList<>();
                    while (rs.next()) {
                        List<String> row = new java.util.ArrayList<>();
                        for (int i = 1; i <= columns.size(); i++) {
                            Object val = rs.getObject(i);
                            row.add(val == null ? null : val.toString());
                        }
                        result.add(row);
                    }
                    return result;
                },
                size, (long) page * size);

        return Map.of(
                "columns", columns,
                "rows", rows,
                "total", total,
                "page", page,
                "size", size);
    }
}
