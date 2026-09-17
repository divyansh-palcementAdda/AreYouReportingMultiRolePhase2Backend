package com.app.AreYouReporting.exceptions;

import java.util.UUID;

public class DepartmentNotFoundException extends ResourceNotFoundException {

    public DepartmentNotFoundException(UUID id) {
        super("Department", "id", id);
    }

    public DepartmentNotFoundException(String message) {
        super(message);
    }
}
