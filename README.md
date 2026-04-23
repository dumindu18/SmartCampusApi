# Smart Campus Sensor & Room Management API

A JAX-RS RESTful API built with Jersey + Grizzly for managing campus rooms and IoT sensors.

---

## Project Structure

```
src/main/java/com/smartcampus/
├── Main.java                          # Entry point, starts Grizzly on port 8080
├── SmartCampusApplication.java        # @ApplicationPath("/api/v1")
├── model/
│   ├── Room.java
│   ├── Sensor.java
│   └── SensorReading.java
├── store/
│   └── DataStore.java                 # Singleton ConcurrentHashMap in-memory store
├── resource/
│   ├── DiscoveryResource.java         # GET /api/v1
│   ├── RoomResource.java              # /api/v1/rooms
│   ├── SensorResource.java            # /api/v1/sensors
│   └── SensorReadingResource.java     # /api/v1/sensors/{id}/readings (sub-resource)
├── exception/
│   ├── RoomNotEmptyException.java
│   ├── LinkedResourceNotFoundException.java
│   ├── SensorUnavailableException.java
│   └── ExceptionMappers.java          # All @Provider exception mappers
└── filter/
    └── LoggingFilter.java             # Request & response logging
```

---

## How to Build & Run

### Prerequisites
- Java 11+
- Maven 3.6+

### Steps

```bash
# 1. Clone the repository
git clone https://github.com/YOUR_USERNAME/smartcampus-api.git
cd smartcampus-api

# 2. Build the fat JAR
mvn package -DskipTests

# 3. Run the server
java -jar target/smartcampus-api-1.0-SNAPSHOT.jar
```

The server starts at: **http://localhost:8080/api/v1**

---

## API Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | /api/v1 | Discovery / API metadata |
| GET | /api/v1/rooms | List all rooms |
| POST | /api/v1/rooms | Create a room |
| GET | /api/v1/rooms/{id} | Get a room by ID |
| DELETE | /api/v1/rooms/{id} | Delete a room (blocked if sensors exist) |
| GET | /api/v1/sensors | List sensors (optional ?type= filter) |
| POST | /api/v1/sensors | Register a sensor |
| GET | /api/v1/sensors/{id} | Get sensor by ID |
| GET | /api/v1/sensors/{id}/readings | Get reading history |
| POST | /api/v1/sensors/{id}/readings | Add a new reading |

---

## Sample curl Commands

### 1. Discovery
```bash
curl -X GET http://localhost:8080/api/v1
```

### 2. Create a Room
```bash
curl -X POST http://localhost:8080/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"HALL-01","name":"Main Hall","capacity":200}'
```

### 3. List All Rooms
```bash
curl -X GET http://localhost:8080/api/v1/rooms
```

### 4. Create a Sensor (linked to existing room)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"CO2-001","type":"CO2","status":"ACTIVE","currentValue":0.0,"roomId":"LIB-301"}'
```

### 5. Filter Sensors by Type
```bash
curl -X GET "http://localhost:8080/api/v1/sensors?type=CO2"
```

### 6. Post a Sensor Reading
```bash
curl -X POST http://localhost:8080/api/v1/sensors/CO2-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'
```

### 7. Get Reading History
```bash
curl -X GET http://localhost:8080/api/v1/sensors/CO2-001/readings
```

### 8. Delete a Room (fails if sensors present → 409)
```bash
curl -X DELETE http://localhost:8080/api/v1/rooms/LIB-301
```

### 9. Create Sensor with Invalid roomId (→ 422)
```bash
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"BAD-001","type":"Temperature","status":"ACTIVE","currentValue":0.0,"roomId":"FAKE-999"}'
```

### 10. Post Reading to MAINTENANCE Sensor (→ 403)
```bash
# First create a sensor with MAINTENANCE status
curl -X POST http://localhost:8080/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"MAINT-001","type":"Temperature","status":"MAINTENANCE","currentValue":0.0,"roomId":"LIB-301"}'

# Then try to post a reading
curl -X POST http://localhost:8080/api/v1/sensors/MAINT-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":22.0}'
```

---

## Report: Answers to Coursework Questions

---

### Part 1.1 – JAX-RS Resource Lifecycle

By default, JAX-RS creates a **new instance of every Resource class for each incoming HTTP request** (per-request scope). This means state stored as instance fields is lost after the request completes. To safely share data across all requests, a **singleton** must be used separately — in this project, `DataStore` is a singleton (`private static final DataStore INSTANCE = new DataStore()`). It uses `ConcurrentHashMap` instead of a regular `HashMap` so that simultaneous requests from multiple threads do not corrupt the shared maps. If a plain `HashMap` were used, concurrent `put` operations could cause data loss or infinite loops due to internal hash collisions during resizing.

---

### Part 1.2 – HATEOAS

**HATEOAS** (Hypermedia as the Engine of Application State) means that API responses include hyperlinks that guide the client to available next actions — similar to how web pages contain links. This is considered an advanced RESTful design principle because the client does not need to hard-code URLs or consult external documentation; the API itself tells the client what it can do next. For example, a response to `GET /api/v1` returns links to `/api/v1/rooms` and `/api/v1/sensors`, meaning the client can discover the whole API from a single entry point. The benefit over static documentation is that if URLs change, clients that follow hypermedia links automatically adapt, whereas clients relying on static docs break immediately.

---

### Part 2.1 – Returning IDs vs Full Room Objects

Returning only IDs (e.g., `["LIB-301","LAB-101"]`) minimises payload size and is efficient when the client only needs to know what rooms exist. However, it forces the client to issue a separate `GET /rooms/{id}` request for each room it wants to display, causing many round-trips and increased latency (the "N+1 problem"). Returning full room objects in the list response is heavier on bandwidth but eliminates those extra calls, making it better when clients need room details immediately (e.g., populating a dashboard table). For most real-world use cases, returning full objects for small-to-medium collections is the preferred approach.

---

### Part 2.2 – Is DELETE Idempotent?

In this implementation, **DELETE is not fully idempotent**. The first `DELETE /rooms/{roomId}` removes the room and returns `204 No Content`. A second identical request returns `404 Not Found` because the room no longer exists. According to the HTTP specification, idempotency means the **server state** is the same after one or many calls — the room is gone in both cases, so the state effect is idempotent. However, the **HTTP response code changes** between calls (204 → 404), which can be considered a soft violation of idempotency from a client experience standpoint. This is a common and accepted trade-off in REST API design.

---

### Part 3.1 – @Consumes and Media Type Mismatch

The `@Consumes(MediaType.APPLICATION_JSON)` annotation tells JAX-RS that this endpoint **only accepts requests whose `Content-Type` header is `application/json`**. If a client sends data with `Content-Type: text/plain` or `Content-Type: application/xml`, JAX-RS automatically returns `415 Unsupported Media Type` without ever invoking the resource method. This is handled entirely by the framework at the routing/matching layer, protecting the method from receiving unparseable data. This makes the API more robust without requiring manual content-type checks inside the method body.

---

### Part 3.2 – @QueryParam vs Path Segment for Filtering

Using `@QueryParam` (e.g., `GET /sensors?type=CO2`) is the standard approach for **filtering and searching** collections because:
1. The base resource URL `/sensors` remains stable and bookmarkable.
2. Multiple filters can be easily combined: `?type=CO2&status=ACTIVE`.
3. It clearly signals to the client that these are optional refinements, not a different resource.

The alternative path approach (`/sensors/type/CO2`) makes filtering look like a separate resource hierarchy, which is semantically incorrect. It also cannot be easily combined with other filters without complex URL design. REST conventions recommend path segments for **resource identity** and query parameters for **optional filtering/sorting/searching**.

---

### Part 4.1 – Sub-Resource Locator Pattern

The Sub-Resource Locator pattern means a parent resource delegates handling of a sub-path to a separate class. In this project, `SensorResource` has a method annotated `@Path("/{sensorId}/readings")` that returns a new `SensorReadingResource` instance — JAX-RS then dispatches the remainder of the request to that class. The benefits are:

1. **Separation of concerns** — each class has a single responsibility (sensors vs readings).
2. **Reduced complexity** — a single massive resource class with dozens of methods is hard to maintain; splitting by sub-resource keeps each class focused.
3. **Testability** — `SensorReadingResource` can be unit-tested independently.
4. **Context injection** — the parent passes relevant context (the `Sensor` object) into the sub-resource constructor, so the sub-resource always operates with the correct scope.

---

### Part 5.2 – HTTP 422 vs 404

`404 Not Found` means the **requested URL/resource does not exist**. However, when a client posts a valid JSON payload to create a sensor but includes a `roomId` that doesn't exist, the URL `/api/v1/sensors` is perfectly valid — the problem is with the **content of the payload**, not the URL itself. `422 Unprocessable Entity` is semantically more accurate because it says: "I understood the request format, but I cannot process it because a referenced entity inside the body is invalid." Using 404 here would mislead clients into thinking the `/sensors` endpoint doesn't exist, when in fact the issue is a broken reference in the request body.

---

### Part 5.4 – Cybersecurity Risk of Exposing Stack Traces

Exposing raw Java stack traces to API consumers is dangerous because:
1. **Package/class names** reveal the internal architecture (e.g., `com.company.db.OracleConnectionPool`), helping attackers understand what technologies are used.
2. **File paths** can expose the server's directory structure.
3. **Library versions** in stack frames let attackers look up known CVEs for that exact version.
4. **Error messages** may contain SQL queries, internal IDs, or credentials accidentally included in exception messages.
5. **Logic flow** is exposed — an attacker can see exactly which code path failed and craft targeted inputs to exploit it.

The `GlobalExceptionMapper` in this project catches all `Throwable` and returns only a generic message, ensuring no internal details leak to the client. Errors are still logged server-side for debugging.

---

### Part 5.5 – Why Use Filters for Cross-Cutting Concerns

Inserting `Logger.info()` manually into every resource method has several drawbacks:
1. **Code duplication** — the same logging boilerplate is copy-pasted into every method.
2. **Inconsistency** — developers may forget to add logging to new methods.
3. **Mixing concerns** — resource methods should focus on business logic, not infrastructure.
4. **Hard to change** — if the log format needs updating, every method must be edited.

JAX-RS filters implement the **Aspect-Oriented Programming (AOP)** principle: cross-cutting concerns (logging, authentication, CORS headers) are defined once in a filter and applied automatically to every request/response. Adding `@Provider` is sufficient — no resource method needs to be modified. This makes the codebase cleaner, more consistent, and much easier to maintain.
