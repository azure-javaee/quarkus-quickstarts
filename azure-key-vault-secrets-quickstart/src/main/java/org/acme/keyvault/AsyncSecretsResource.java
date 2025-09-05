package org.acme.keyvault;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import com.azure.security.keyvault.secrets.SecretAsyncClient;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

/**
 * This resource class provides an asynchronous API for Azure Key Vault Secrets.
 */
@Path("/secrets/async")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AsyncSecretsResource {
    
    private static final Logger LOG = Logger.getLogger(AsyncSecretsResource.class);
    
    @Inject
    SecretAsyncClient secretAsyncClient;

    /**
     * Retrieves a secret asynchronously from Azure Key Vault
     * 
     * @param secretName the name of the secret to retrieve
     * @return a CompletionStage with the secret value
     */
    @GET
    @Path("/{secretName}")
    public CompletionStage<Response> getSecret(@PathParam("secretName") String secretName) {
        LOG.debugf("Getting secret asynchronously: %s", secretName);
        
        return secretAsyncClient.getSecret(secretName)
            .map(secret -> Response.ok(new SecretResponse(secret.getName(), secret.getValue())).build())
            .onErrorReturn(error -> {
                LOG.errorf("Error retrieving secret: %s - %s", secretName, error.getMessage());
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Secret not found or access denied: " + secretName))
                    .build();
            })
            .toFuture();
    }

    /**
     * Creates or updates a secret asynchronously in Azure Key Vault
     * 
     * @param secretRequest the secret to create or update
     * @return a CompletionStage with the created/updated secret
     */
    @POST
    public CompletionStage<Response> createSecret(SecretRequest secretRequest) {
        LOG.debugf("Creating/updating secret asynchronously: %s", secretRequest.getName());
        
        return secretAsyncClient.setSecret(secretRequest.getName(), secretRequest.getValue())
            .map(secret -> Response.status(Response.Status.CREATED)
                .entity(new SecretResponse(secret.getName(), secret.getValue()))
                .build())
            .onErrorReturn(error -> {
                LOG.errorf("Error creating/updating secret: %s - %s", secretRequest.getName(), error.getMessage());
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorResponse("Error creating/updating secret: " + error.getMessage()))
                    .build();
            })
            .toFuture();
    }

    /**
     * Deletes a secret asynchronously from Azure Key Vault
     * 
     * @param secretName the name of the secret to delete
     * @return a CompletionStage with a response indicating success or failure
     */
    @DELETE
    @Path("/{secretName}")
    public CompletionStage<Response> deleteSecret(@PathParam("secretName") String secretName) {
        LOG.debugf("Deleting secret asynchronously: %s", secretName);
        
        CompletableFuture<Response> result = new CompletableFuture<>();
        
        secretAsyncClient.beginDeleteSecret(secretName)
            .subscribe(
                poller -> {
                    try {
                        poller.getFinalResult().toFuture().get(); // Wait for completion
                        result.complete(Response.ok(new MessageResponse("Secret deleted: " + secretName)).build());
                    } catch (InterruptedException | ExecutionException e) {
                        LOG.errorf("Error waiting for secret deletion: %s - %s", secretName, e.getMessage());
                        result.complete(Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                            .entity(new ErrorResponse("Error deleting secret: " + e.getMessage()))
                            .build());
                    }
                },
                error -> {
                    LOG.errorf("Error deleting secret: %s - %s", secretName, error.getMessage());
                    result.complete(Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("Error deleting secret: " + error.getMessage()))
                        .build());
                }
            );
        
        return result;
    }

    /**
     * Lists all secrets asynchronously in the Azure Key Vault
     * 
     * @return a CompletionStage with a list of secret names
     */
    @GET
    public CompletionStage<Response> listSecrets() {
        LOG.debug("Listing all secrets asynchronously");
        
        return secretAsyncClient.listPropertiesOfSecrets()
            .map(secretProperties -> secretProperties.getName())
            .collectList()
            .map(secretNames -> Response.ok(secretNames).build())
            .onErrorReturn(error -> {
                LOG.errorf("Error listing secrets: %s", error.getMessage());
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("Error listing secrets: " + error.getMessage()))
                    .build();
            })
            .toFuture();
    }
}