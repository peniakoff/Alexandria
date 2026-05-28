package pl.tomaszmiller.api;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/health")
class HealthController {

    @Get(produces = MediaType.TEXT_PLAIN)
    String health() {
        return "ok";
    }
}

