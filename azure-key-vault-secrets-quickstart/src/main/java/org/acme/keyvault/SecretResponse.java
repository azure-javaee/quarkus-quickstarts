package org.acme.keyvault;

/**
 * Response model for returning a secret
 */
public class SecretResponse {
    private String name;
    private String value;

    public SecretResponse() {
    }

    public SecretResponse(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}