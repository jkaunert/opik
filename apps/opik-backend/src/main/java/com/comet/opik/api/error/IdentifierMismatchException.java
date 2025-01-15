package com.comet.opik.api.error;

import io.dropwizard.jersey.errors.ErrorMessage;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.core.Response;

public class IdentifierMismatchException extends ClientErrorException {
    public IdentifierMismatchException(String message) {
        super(Response.status(Response.Status.CONFLICT)
                .entity(new ErrorMessage(Response.Status.CONFLICT.getStatusCode(), message))
                .build());
    }
}
