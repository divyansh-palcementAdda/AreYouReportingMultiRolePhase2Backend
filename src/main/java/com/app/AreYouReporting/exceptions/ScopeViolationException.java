package com.app.AreYouReporting.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ScopeViolationException extends RuntimeException {

    public ScopeViolationException(String message) {
        super(message);
    }
}
