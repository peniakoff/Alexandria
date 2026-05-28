package pl.tomaszmiller.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import pl.tomaszmiller.model.BookMetadata;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenLibraryServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void lookupByIsbn_mapsOpenLibraryResponse() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/books", exchange -> {
            String body = """
                    {
                      "ISBN:9780140328721": {
                        "title": "Matilda",
                        "number_of_pages": 240,
                        "publish_date": "1988",
                        "authors": [{"name": "Roald Dahl"}],
                        "publishers": [{"name": "Puffin"}]
                      }
                    }
                    """;
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.getBytes(StandardCharsets.UTF_8).length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();

        OpenLibraryService service = new OpenLibraryService(
                HttpClient.newHttpClient(), "http://localhost:" + server.getAddress().getPort());

        Optional<BookMetadata> result = service.lookupByIsbn("978-0-14-032872-1");

        assertTrue(result.isPresent());
        assertEquals("Matilda", result.get().title());
        assertEquals("Roald Dahl", result.get().author());
        assertEquals(240, result.get().pages());
        assertEquals(1988, result.get().publishYear());
        assertEquals("Puffin", result.get().publisher());
        assertEquals("9780140328721", result.get().isbn());
    }

    @Test
    void lookupByIsbn_returnsEmptyForMissingBook() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/books", exchange -> {
            String body = "{}";
            exchange.sendResponseHeaders(200, body.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();

        OpenLibraryService service = new OpenLibraryService(
                HttpClient.newHttpClient(), "http://localhost:" + server.getAddress().getPort());

        assertTrue(service.lookupByIsbn("1234567890").isEmpty());
    }
}
