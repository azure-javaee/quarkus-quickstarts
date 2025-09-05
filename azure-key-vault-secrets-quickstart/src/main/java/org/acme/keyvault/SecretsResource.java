package org.acme.keyvault;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;

import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;

/**
 * This resource class provides a synchronous API for Azure Key Vault Secrets.
 */
@Path("/secrets/sync")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SecretsResource {
    
    private static final Logger LOG = Logger.getLogger(SecretsResource.class);

    @Inject
    SecretClient secretClient;

    /**
     * Retrieves a secret from Azure Key Vault
     * 
     * @param secretName the name of the secret to retrieve
     * @return the secret value
     */
    @GET
    @Path("/{secretName}")
    public Response getSecret(@PathParam("secretName") String secretName) {
        LOG.debugf("Getting secret: %s", secretName);
        try {
            KeyVaultSecret secret = secretClient.getSecret(secretName);
            return Response.ok(new SecretResponse(secret.getName(), secret.getValue())).build();
        } catch (Exception e) {
            LOG.errorf("Error retrieving secret: %s - %s", secretName, e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Secret not found or access denied: " + secretName))
                .build();
        }
    }

    /**
     * Creates or updates a secret in Azure Key Vault
     * 
     * @param secretRequest the secret to create or update
     * @return the created/updated secret
     */
    @POST
    public Response createSecret(SecretRequest secretRequest) {
        LOG.debugf("Creating/updating secret: %s", secretRequest.getName());
        try {
            KeyVaultSecret secret = secretClient.setSecret(secretRequest.getName(), secretRequest.getValue());
            return Response.status(Response.Status.CREATED)
                .entity(new SecretResponse(secret.getName(), secret.getValue()))
                .build();
        } catch (Exception e) {
            LOG.errorf("Error creating/updating secret: %s - %s", secretRequest.getName(), e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Error creating/updating secret: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Deletes a secret from Azure Key Vault
     * 
     * @param secretName the name of the secret to delete
     * @return a response indicating success or failure
     */
    @DELETE
    @Path("/{secretName}")
    public Response deleteSecret(@PathParam("secretName") String secretName) {
        LOG.debugf("Deleting secret: %s", secretName);
        try {
            secretClient.beginDeleteSecret(secretName).waitForCompletion();
            return Response.ok(new MessageResponse("Secret deleted: " + secretName)).build();
        } catch (Exception e) {
            LOG.errorf("Error deleting secret: %s - %s", secretName, e.getMessage());
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse("Error deleting secret: " + e.getMessage()))
                .build();
        }
    }

    /**
     * Lists all secrets in the Azure Key Vault
     * 
     * @return a list of secret names
     */
    @GET
    public Response listSecrets() {
        LOG.debug("Listing all secrets");
        try {
            var secretNames = secretClient.listPropertiesOfSecrets().stream()
                    .map(secretProperties -> secretProperties.getName())
                    .toList();
            return Response.ok(secretNames).build();
        } catch (Exception e) {
            LOG.errorf("Error listing secrets: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Error listing secrets: " + e.getMessage()))
                .build();
        }
    }
}