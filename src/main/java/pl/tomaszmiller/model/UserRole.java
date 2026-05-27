package pl.tomaszmiller.model;

/** Role assigned to a library user. */
public enum UserRole {
    /** Regular library member with borrowing privileges. */
    USER(0),
    /** Library administrator with full management access. */
    ADMIN(1);

    private final int dbValue;

    UserRole(int dbValue) {
        this.dbValue = dbValue;
    }

    public int getDbValue() {
        return dbValue;
    }

    public static UserRole fromDbValue(int value) {
        return switch (value) {
            case 1 -> ADMIN;
            default -> USER;
        };
    }
}
