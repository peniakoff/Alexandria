package pl.tomaszmiller.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class KeycloakPasswordGrantClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final String tokenUrl;
    private final String clientId;
    private final String clientSecret;

    public KeycloakPasswordGrantClient(String issuerUrl, String clientId, String clientSecret) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.tokenUrl = normalizeIssuer(issuerUrl) + "/protocol/openid-connect/token";
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String loginAndGetAccessToken(String username, String password) throws Exception {
        String form = "grant_type=password"
                + "&client_id=" + enc(clientId)
                + (clientSecret != null && !clientSecret.isBlank() ? "&client_secret=" + enc(clientSecret) : "")
                + "&username=" + enc(username)
                + "&password=" + enc(password);

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .timeout(TIMEOUT)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();

        HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() / 100 != 2) {
            throw new Exception("OIDC login failed " + resp.statusCode() + ": " + resp.body());
        }

        JsonObject obj = JsonParser.parseString(resp.body()).getAsJsonObject();
        if (!obj.has("access_token")) {
            throw new Exception("OIDC login response missing access_token");
        }
        return obj.get("access_token").getAsString();
    }

    private static String enc(String v) {
        return URLEncoder.encode(v, StandardCharsets.UTF_8);
    }

    private static String normalizeIssuer(String issuerUrl) {
        String trimmed = issuerUrl == null ? "" : issuerUrl.trim();
        if (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}

