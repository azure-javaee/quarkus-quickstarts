# Quarkus Azure Key Vault Secrets Quickstart

This quickstart demonstrates how to use the Quarkus Azure Key Vault extension to interact with Azure Key Vault secrets. The application provides REST endpoints for managing secrets in an Azure Key Vault using both synchronous and asynchronous clients, as well as configuration-based secret access.

## Prerequisites

* JDK 21
* Apache Maven 3.8+
* Azure CLI installed and configured
* Azure account with an active subscription
* Docker (for native compilation)

## Azure Key Vault Setup

Before running the application, you need to create an Azure Key Vault and configure access permissions.

### 1. Log in to Azure

```bash
az login
```

### 2. Create a Resource Group

```bash
RESOURCE_GROUP_NAME=quarkus-keyvault-demo
LOCATION=eastus

# Create a resource group
az group create --name $RESOURCE_GROUP_NAME --location $LOCATION
```

### 3. Create an Azure Key Vault

```bash
# Create a unique name for your Key Vault
KEY_VAULT_NAME=quarkus-kv-$(date +%s)

# Create the Key Vault
az keyvault create --name $KEY_VAULT_NAME \
    --resource-group $RESOURCE_GROUP_NAME \
    --location $LOCATION \
    --enable-rbac-authorization false
```

### 4. Set Up Access Permissions

The Azure Key Vault extension uses the Azure Identity library to authenticate with Key Vault. The application can use Azure CLI credentials when run locally.

If using Azure RBAC, assign the appropriate role to your user:

```bash
# Get your user principal name
USER_NAME=$(az ad signed-in-user show --query userPrincipalName -o tsv)

# Assign the Key Vault Secrets Officer role
az role assignment create \
    --role "Key Vault Secrets Officer" \
    --assignee "$USER_NAME" \
    --scope "/subscriptions/$(az account show --query id -o tsv)/resourceGroups/$RESOURCE_GROUP_NAME/providers/Microsoft.KeyVault/vaults/$KEY_VAULT_NAME"
```

### 5. Create an Initial Secret

```bash
az keyvault secret set --vault-name $KEY_VAULT_NAME --name "example-secret" --value "This is a secret value from Azure Key Vault"
```

### 6. Get the Key Vault URI

```bash
KEY_VAULT_URI=$(az keyvault show --name $KEY_VAULT_NAME \
    --resource-group $RESOURCE_GROUP_NAME \
    --query properties.vaultUri -o tsv)
    
echo "Key Vault URI: $KEY_VAULT_URI"
```

You'll need this URI to configure the application.

## Running the Application

### 1. Configure the Application

Edit the `src/main/resources/application.properties` file to set the Key Vault endpoint:

```properties
quarkus.azure.keyvault.secret.endpoint=${KEY_VAULT_URI}
```

Or, set the endpoint as an environment variable when running the application:

```bash
export QUARKUS_AZURE_KEYVAULT_SECRET_ENDPOINT=$KEY_VAULT_URI
```

### 2. Running in Development Mode

```bash
./mvnw quarkus:dev
```

This will start the application in development mode with hot reload enabled.

### 3. Building and Running in JVM Mode

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

### 4. Building and Running as a Native Executable

```bash
./mvnw package -Dnative -Dquarkus.native.container-build=true
./target/azure-key-vault-secrets-quickstart-1.0.0-SNAPSHOT-runner
```

## Testing the API

Once the application is running, you can test the various endpoints with curl or your preferred API testing tool:

### Synchronous API (SecretClient)

List all secrets:
```bash
curl -X GET http://localhost:8080/secrets/sync
```

Get a specific secret:
```bash
curl -X GET http://localhost:8080/secrets/sync/example-secret
```

Create or update a secret:
```bash
curl -X POST http://localhost:8080/secrets/sync \
  -H "Content-Type: application/json" \
  -d '{"name":"test-secret","value":"My test secret value"}'
```

Delete a secret:
```bash
curl -X DELETE http://localhost:8080/secrets/sync/test-secret
```

### Asynchronous API (SecretAsyncClient)

List all secrets:
```bash
curl -X GET http://localhost:8080/secrets/async
```

Get a specific secret:
```bash
curl -X GET http://localhost:8080/secrets/async/example-secret
```

Create or update a secret:
```bash
curl -X POST http://localhost:8080/secrets/async \
  -H "Content-Type: application/json" \
  -d '{"name":"async-secret","value":"My async test secret value"}'
```

Delete a secret:
```bash
curl -X DELETE http://localhost:8080/secrets/async/async-secret
```

### Configuration-based Secret Access

Get a configured example secret:
```bash
curl -X GET http://localhost:8080/secrets/config/example
```

Get a specific secret by name:
```bash
curl -X GET http://localhost:8080/secrets/config/test-secret
```

## How it Works

The quickstart demonstrates three different ways to interact with Azure Key Vault secrets:

1. **Synchronous Client**: Uses the `SecretClient` to perform blocking operations.
2. **Asynchronous Client**: Uses the `SecretAsyncClient` for non-blocking operations.
3. **Configuration-based Access**: Uses the Quarkus configuration system to access secrets via the `@ConfigProperty` annotation or `ConfigProvider`.

### Key Components

- `SecretsResource.java`: Provides REST endpoints for synchronous operations
- `AsyncSecretsResource.java`: Provides REST endpoints for asynchronous operations
- `ConfigSecretResource.java`: Demonstrates accessing secrets via configuration
- Model classes for handling requests and responses

## Cleaning Up

When you're done with the quickstart, you can clean up the Azure resources:

```bash
az group delete --name $RESOURCE_GROUP_NAME --yes
```

## Additional Resources

- [Quarkus Azure Services Extension Documentation](https://docs.quarkiverse.io/quarkus-azure-services/dev/index.html)
- [Azure Key Vault Documentation](https://docs.microsoft.com/en-us/azure/key-vault/)
- [Azure Identity for Java](https://docs.microsoft.com/en-us/java/api/overview/azure/identity-readme)