package com.requillion.solutions.inventory.security;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class UserInfo {

    @JsonProperty("sub")
    private String sub;

    @JsonProperty("family_name")
    private String familyName;

    @JsonProperty("given_name")
    private String givenName;

    @JsonProperty("email")
    private String email;

    @JsonProperty("preferred_username")
    private String preferredUsername;

    public UUID getSubAsUUID() {
        return UUID.fromString(this.sub);
    }
}
