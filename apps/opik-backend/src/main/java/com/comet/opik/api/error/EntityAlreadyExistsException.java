package com.comet.opik.api.error;

import io.dropwizard.jersey.errors.ErrorMessage;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class EntityAlreadyExistsException extends ClientErrorException {

    public EntityAlreadyExistsException(String message) {
        super(Response.status(Response.Status.CONFLICT)
                .entity(new ErrorMessage(Response.Status.CONFLICT.getStatusCode(), message))
                .build());
    }
}
