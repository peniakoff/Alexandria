package pl.tomaszmiller.config;

/**
 * Supported data-source backends for Alexandria.
 *
 * <ul>
 *   <li>{@link #MYSQL}   – remote MySQL/MariaDB server via JDBC (default)</li>
 *   <li>{@link #SQLITE}  – local SQLite file via JDBC (offline/private libraries)</li>
 *   <li>{@link #REST_API}– cloud REST API, e.g. Supabase (SaaS deployments)</li>
 * </ul>
 * <p>
 * Configured via {@code alexandria.datasource.type} property or
 * {@code ALEXANDRIA_DATASOURCE_TYPE} environment variable.
 */
public enum DatasourceType {
    MYSQL,
    SQLITE,
    REST_API;

    public static DatasourceType fromString(String value, DatasourceType defaultType) {
        if (value == null || value.isBlank()) {
            return defaultType;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultType;
        }
    }
}
