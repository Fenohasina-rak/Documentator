package org.acme.Services.Gemini;


import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.acme.Models.Gemini.GeminiRequest;
import org.acme.Models.Gemini.GeminiResponse;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "gemini-api")
public interface GeminiClient {

    @POST
    @Path("/v1beta/models/{model}:generateContent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    GeminiResponse generateContent(
            @PathParam("model") String model,
            @QueryParam("key") String apiKey,
            GeminiRequest request
    );
}
