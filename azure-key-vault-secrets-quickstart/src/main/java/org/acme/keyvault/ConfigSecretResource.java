package org.acme.keyvault;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

/**
 * This resource class demonstrates accessing Azure Key Vault secrets through the Quarkus configuration system.
 */
@Path("/secrets/config")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigSecretResource {
    
    private static final Logger LOG = Logger.getLogger(ConfigSecretResource.class);

    // The 'kv//' prefix allows accessing secrets directly by name in Key Vault
    @ConfigProperty(name = "kv//example-secret", defaultValue = "Secret not found")
    String exampleSecret;
    
    /**
     * Retrieves a secret configured via Quarkus configuration
     * 
     * @return the example secret value
     */
    @GET
    @Path("/example")
    public Response getExampleSecret() {
        LOG.debug("Getting example secret from config");
        return Response.ok(new SecretResponse("example-secret", exampleSecret)).build();
    }
    
    /**
     * Retrieves a secret by name using ConfigProvider
     * 
     * @param secretName the name of the secret to retrieve
     * @return the secret value
     */
    @GET
    @Path("/{secretName}")
    public Response getSecretByName(@PathParam("secretName") String secretName) {
        LOG.debugf("Getting secret from config: %s", secretName);
        
        try {
            // Demonstrate how to access dynamic Key Vault secrets
            String value = org.eclipse.microprofile.config.ConfigProvider.getConfig()
                    .getValue("kv//" + secretName, String.class);
            
            return Response.ok(new SecretResponse(secretName, value)).build();
        } catch (Exception e) {
            LOG.errorf("Error retrieving secret from config: %s - %s", secretName, e.getMessage());
            return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Secret not found or access denied: " + secretName))
                .build();
        }
    }
}