package io.github.hectorvent.floci.lifecycle;

import io.github.hectorvent.floci.core.common.ServiceRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Internal health endpoint at /_floci/health.
 * Returns the Floci version and the status of each enabled service.
 * Compatible with the LocalStack /_localstack/health pattern.
 */
@Path("/_floci/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthController {

    private final ServiceRegistry serviceRegistry;
    private final String version;

    @Inject
    public HealthController(ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        this.version = resolveVersion();
    }

    @GET
    public Response health() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("version", version);
        result.put("edition", "community");

        Map<String, String> services = new LinkedHashMap<>();
        for (String service : serviceRegistry.getEnabledServices()) {
            services.put(service, "available");
        }
        result.put("services", services);

        return Response.ok(result).build();
    }

    private static String resolveVersion() {
        String env = System.getenv("FLOCI_VERSION");
        if (env != null && !env.isBlank()) {
            return env;
        }
        return "dev";
    }
}
