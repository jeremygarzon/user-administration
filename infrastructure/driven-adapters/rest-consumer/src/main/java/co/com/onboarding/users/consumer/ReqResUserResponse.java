
package co.com.onboarding.users.consumer;


import com.fasterxml.jackson.annotation.JsonProperty;

public record ReqResUserResponse(Data data) {

    public record Data(
            Long id,
            String email,
            @JsonProperty("first_name")
            String firstName,
            @JsonProperty("last_name")
            String lastName,
            String avatar
    ) {}
}
