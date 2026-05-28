package pl.tomaszmiller.auth;

public final class DesktopAuthSession {

    private static volatile String accessToken;

    private DesktopAuthSession() {
    }

    public static String getAccessToken() {
        return accessToken;
    }

    public static void setAccessToken(String token) {
        accessToken = token;
    }

    public static void clear() {
        accessToken = null;
    }
}

