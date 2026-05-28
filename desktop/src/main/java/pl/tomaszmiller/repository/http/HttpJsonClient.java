package pl.tomaszmiller.repository.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class HttpJsonClient {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final String baseUrl;
    private final HttpClient httpClient;
    final Gson gson;

    HttpJsonClient(String baseUrl) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.httpClient = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
        this.gson = new GsonBuilder().create();
    }

    HttpResponse<String> get(String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .GET();
        applyAuth(b);
        HttpRequest req = b.build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> post(String path, String jsonBody) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        applyAuth(b);
        HttpRequest req = b.build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> put(String path, String jsonBody) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody));
        applyAuth(b);
        HttpRequest req = b.build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> delete(String path) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .timeout(TIMEOUT)
                .DELETE();
        applyAuth(b);
        HttpRequest req = b.build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    static void ensure2xx(HttpResponse<String> resp) throws Exception {
        if (resp.statusCode() / 100 != 2) {
            throw new Exception("API error " + resp.statusCode() + ": " + resp.body());
        }
    }

    private void applyAuth(HttpRequest.Builder b) {
        String token = pl.tomaszmiller.auth.DesktopAuthSession.getAccessToken();
        if (token != null && !token.isBlank()) {
            b.header("Authorization", "Bearer " + token.trim());
        }
    }
}

