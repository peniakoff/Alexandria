package pl.tomaszmiller.repository.port;

import pl.tomaszmiller.model.ExtensionRequest;
import pl.tomaszmiller.model.RequestStatus;

import java.util.List;
import java.util.Optional;

/**
 * Port for extension request persistence.
 */
public interface ExtensionRequestRepository {
    List<ExtensionRequest> findAll() throws Exception;

    List<ExtensionRequest> findByStatus(RequestStatus status) throws Exception;

    List<ExtensionRequest> findByUserId(long userId) throws Exception;

    Optional<ExtensionRequest> findById(long id) throws Exception;

    ExtensionRequest save(ExtensionRequest request) throws Exception;

    void updateStatus(long id, RequestStatus status) throws Exception;
}
