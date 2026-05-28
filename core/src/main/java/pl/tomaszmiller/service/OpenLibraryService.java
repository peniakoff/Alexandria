package pl.tomaszmiller.service;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import pl.tomaszmiller.model.BookMetadata;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lightweight OpenLibrary integration for ISBN lookups.
 */
public class OpenLibraryService {

    private static final Pattern YEAR_PATTERN = Pattern.compile("(\\d{4})");
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final String apiBaseUrl;

    public OpenLibraryService() {
        this(HttpClient.newBuilder().connectTimeout(TIMEOUT).build(), "https://openlibrary.org");
    }

    OpenLibraryService(HttpClient httpClient, String apiBaseUrl) {
        this.httpClient = httpClient;
        this.apiBaseUrl = apiBaseUrl;
    }

    public Optional<BookMetadata> lookupByIsbn(String rawIsbn) {
        String normalized = normalizeIsbn(rawIsbn);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        try {
            String url = apiBaseUrl + "/api/books?bibkeys=ISBN:" + URLEncoder.encode(normalized, StandardCharsets.UTF_8)
                    + "&format=json&jscmd=data";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                return Optional.empty();
            }
            JsonObject root = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonElement entry = root.get("ISBN:" + normalized);
            if (entry == null || !entry.isJsonObject()) {
                return Optional.empty();
            }
            JsonObject data = entry.getAsJsonObject();
            return Optional.of(new BookMetadata(
                    extractFirstName(data.getAsJsonArray("authors")),
                    getString(data, "title"),
                    data.has("number_of_pages") ? data.get("number_of_pages").getAsInt() : 0,
                    normalized,
                    extractYear(getString(data, "publish_date")),
                    extractFirstName(data.getAsJsonArray("publishers"))
            ));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private String normalizeIsbn(String rawIsbn) {
        return rawIsbn == null ? "" : rawIsbn.replaceAll("[^0-9Xx]", "").trim();
    }

    private String extractFirstName(JsonArray array) {
        if (array == null || array.isEmpty()) {
            return "";
        }
        JsonElement first = array.get(0);
        if (!first.isJsonObject()) {
            return first.getAsString();
        }
        JsonObject object = first.getAsJsonObject();
        return getString(object, "name");
    }

    private int extractYear(String publishDate) {
        if (publishDate == null) {
            return 0;
        }
        Matcher matcher = YEAR_PATTERN.matcher(publishDate);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private String getString(JsonObject object, String key) {
        return object.has(key) && !object.get(key).isJsonNull() ? object.get(key).getAsString() : "";
    }
}
