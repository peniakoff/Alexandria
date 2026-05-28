package pl.tomaszmiller.api;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import pl.tomaszmiller.model.User;
import pl.tomaszmiller.repository.port.UserRepository;

import java.util.List;
import java.util.Optional;

@Controller("/api/users")
@Secured(SecurityRule.IS_AUTHENTICATED)
class UsersController {

    private final UserRepository userRepository;

    UsersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Get
    @Secured("ADMIN")
    List<User> list() throws Exception {
        return userRepository.findAll();
    }

    @Get("/{id}")
    HttpResponse<User> byId(@PathVariable long id) throws Exception {
        Optional<User> user = userRepository.findById(id);
        return user.map(HttpResponse::ok).orElseGet(HttpResponse::notFound);
    }

    @Get("/by-email{?email}")
    HttpResponse<User> byEmail(@QueryValue String email) throws Exception {
        Optional<User> user = userRepository.findByEmail(email);
        return user.map(HttpResponse::ok).orElseGet(HttpResponse::notFound);
    }

    @Get("/exists{?email}")
    @Secured(SecurityRule.IS_ANONYMOUS)
    boolean exists(@QueryValue String email) throws Exception {
        return userRepository.existsByEmail(email);
    }

    @Post
    @Secured(SecurityRule.IS_ANONYMOUS)
    User create(@Body CreateUserRequest req) throws Exception {
        return userRepository.save(req.user(), req.passwordHash());
    }

    @Put("/{id}")
    HttpResponse<?> update(@PathVariable long id, @Body User user) throws Exception {
        User toUpdate = new User(id, user.firstName(), user.lastName(), user.email(), user.phoneNumber(), user.role());
        userRepository.update(toUpdate);
        return HttpResponse.noContent();
    }

    @Put("/{id}/password")
    HttpResponse<?> updatePassword(@PathVariable long id, @Body UpdatePasswordRequest req) throws Exception {
        userRepository.updatePassword(id, req.passwordHash());
        return HttpResponse.noContent();
    }

    @Delete("/{id}")
    @Secured("ADMIN")
    HttpResponse<?> delete(@PathVariable long id) throws Exception {
        userRepository.deleteById(id);
        return HttpResponse.noContent();
    }

    record CreateUserRequest(User user, String passwordHash) {
    }

    record UpdatePasswordRequest(String passwordHash) {
    }
}

