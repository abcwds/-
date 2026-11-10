package com.pet.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(0)
public class DatabaseMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        addColumns("pet", List.of(
                new Column("source_name", "VARCHAR(100)"),
                new Column("source_url", "VARCHAR(500)"),
                new Column("source_checked_at", "DATE"),
                new Column("image_source_name", "VARCHAR(100)"),
                new Column("image_source_url", "VARCHAR(500)"),
                new Column("source_note", "VARCHAR(500)")
        ));
        addColumns("health_record", List.of(
                new Column("source_name", "VARCHAR(100)"),
                new Column("source_url", "VARCHAR(500)"),
                new Column("source_note", "VARCHAR(500)")
        ));
        addColumns("vaccine_record", List.of(
                new Column("source_name", "VARCHAR(100)"),
                new Column("source_url", "VARCHAR(500)"),
                new Column("source_note", "VARCHAR(500)")
        ));
    }

    private void addColumns(String table, List<Column> columns) {
        for (Column column : columns) {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.COLUMNS " +
                            "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                    Integer.class,
                    table,
                    column.name());
            if (count != null && count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column.name() + " " + column.definition());
            }
        }
    }

    private record Column(String name, String definition) {
    }
}
