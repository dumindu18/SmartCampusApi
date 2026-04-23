package com.smartcampus;

import com.smartcampus.exception.GlobalExceptionMapper;
import com.smartcampus.exception.LinkedResourceNotFoundExceptionMapper;
import com.smartcampus.exception.RoomNotEmptyExceptionMapper;
import com.smartcampus.exception.SensorUnavailableExceptionMapper;
import com.smartcampus.filter.LoggingFilter;
import com.smartcampus.resource.DiscoveryResource;
import com.smartcampus.resource.RoomResource;
import com.smartcampus.resource.SensorResource;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class Main {
    public static final String BASE_URI = "http://0.0.0.0:8080/api/v1/";

    public static void main(String[] args) throws Exception {
        ResourceConfig config = new ResourceConfig();

        // Register resources
        config.register(DiscoveryResource.class);
        config.register(RoomResource.class);
        config.register(SensorResource.class);

        // Register exception mappers explicitly
        config.register(RoomNotEmptyExceptionMapper.class);
        config.register(LinkedResourceNotFoundExceptionMapper.class);
        config.register(SensorUnavailableExceptionMapper.class);
        config.register(GlobalExceptionMapper.class);

        // Register filter
        config.register(LoggingFilter.class);

        // Register Jackson for JSON
        config.register(JacksonFeature.class);

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
        System.out.println("Smart Campus API started at http://localhost:8080/api/v1");
        System.out.println("Press CTRL+C to stop...");
        Thread.currentThread().join();
    }
}
