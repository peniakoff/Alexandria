package pl.tomaszmiller.api;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest
class ApiIntegrationTest {

    @Inject
    @Client("/")
    HttpClient client;

    @Test
    void healthShouldReturnOk() {
        String body = client.toBlocking().retrieve(HttpRequest.GET("/health"));
        assertEquals("ok", body);
    }
}

