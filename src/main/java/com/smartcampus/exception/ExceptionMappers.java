package com.smartcampus.exception;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.HashMap;
import java.util.Map;

// ---- 409 Conflict: Room still has sensors ----
@Provider
class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {
    @Override
    public Response toResponse(RoomNotEmptyException e) {
        Map<String, String> body = new HashMap<>();
        body.put("error", e.getMessage());
        body.put("status", "409 Conflict");
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}

// ---- 422 Unprocessable Entity: roomId reference doesn't exist ----
@Provider
class LinkedResourceNotFoundExceptionMapper implements ExceptionMapper<LinkedResourceNotFoundException> {
    @Override
    public Response toResponse(LinkedResourceNotFoundException e) {
        Map<String, String> body = new HashMap<>();
        body.put("error", e.getMessage());
        body.put("status", "422 Unprocessable Entity");
        return Response.status(422)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}

// ---- 403 Forbidden: Sensor is under MAINTENANCE ----
@Provider
class SensorUnavailableExceptionMapper implements ExceptionMapper<SensorUnavailableException> {
    @Override
    public Response toResponse(SensorUnavailableException e) {
        Map<String, String> body = new HashMap<>();
        body.put("error", e.getMessage());
        body.put("status", "403 Forbidden");
        return Response.status(Response.Status.FORBIDDEN)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}

// ---- 500 Global catch-all for any unexpected exception ----
@Provider
class GlobalExceptionMapper implements ExceptionMapper<Throwable> {
    @Override
    public Response toResponse(Throwable e) {
        Map<String, String> body = new HashMap<>();
        body.put("error", "An unexpected internal error occurred. Please contact the administrator.");
        body.put("status", "500 Internal Server Error");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .type(MediaType.APPLICATION_JSON)
                .entity(body).build();
    }
}
