package com.app.AreYouReporting.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ProofValidationException extends RuntimeException {

    private final List<String> missingRequirements;

    public ProofValidationException(String message) {
        super(message);
        this.missingRequirements = List.of();
    }

    public ProofValidationException(String message, List<String> missingRequirements) {
        super(message);
        this.missingRequirements = missingRequirements != null ? missingRequirements : List.of();
    }

    public List<String> getMissingRequirements() {
        return missingRequirements;
    }
}
