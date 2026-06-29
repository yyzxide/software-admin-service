package com.xcappstore.admin.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class MySqlSchemaIntegrationTest {
    @Container
    private static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
        .withDatabaseName("db_java_software_admin")
        .withUsername("test")
        .withPassword("test");

    @Test
    void categoryAliveNameUniqueIndexAllowsReuseAfterSoftDelete() throws Exception {
        try (Connection connection = DriverManager.getConnection(
            MYSQL.getJdbcUrl(),
            MYSQL.getUsername(),
            MYSQL.getPassword()
        )) {
            ScriptUtils.executeSqlScript(
                connection,
                new EncodedResource(new ByteArrayResource(schemaSql().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8),
                false,
                false,
                ScriptUtils.DEFAULT_COMMENT_PREFIX,
                ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
                ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER
            );

            SQLException duplicate = assertThrows(
                SQLException.class,
                () -> insertCategory(connection, "办公软件")
            );
            assertEquals("23000", duplicate.getSQLState());

            connection.createStatement().executeUpdate("UPDATE categories SET deleted_at = NOW(3) WHERE name = '办公软件'");
            insertCategory(connection, "办公软件");

            long aliveCount = aliveCategoryCount(connection, "办公软件");
            assertEquals(1L, aliveCount);
        }
    }

    private String schemaSql() throws Exception {
        Path modulePath = Path.of("../database/mysql/001_init_admin_schema.sql");
        Path rootPath = Path.of("database/mysql/001_init_admin_schema.sql");
        Path schemaPath = Files.exists(modulePath) ? modulePath : rootPath;
        String sql = Files.readString(schemaPath, StandardCharsets.UTF_8);
        String useStatement = "USE db_java_software_admin;";
        int useIndex = sql.indexOf(useStatement);
        return useIndex < 0 ? sql : sql.substring(useIndex + useStatement.length());
    }

    private void insertCategory(Connection connection, String name) throws SQLException {
        try (var statement = connection.prepareStatement(
            "INSERT INTO categories (name, description, icon, parent_id, sort_order, status, is_builtin) VALUES (?, '', '', NULL, 0, 1, 0)"
        )) {
            statement.setString(1, name);
            statement.executeUpdate();
        }
    }

    private long aliveCategoryCount(Connection connection, String name) throws SQLException {
        try (var statement = connection.prepareStatement(
            "SELECT COUNT(1) FROM categories WHERE name = ? AND deleted_at IS NULL"
        )) {
            statement.setString(1, name);
            try (var resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        }
    }
}
