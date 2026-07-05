package com.patientsystem.organizationservice.service;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminUpdateUserAttributesRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AttributeType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CognitoService {

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;
    private final CognitoIdentityProviderClient cognitoClient;

    public CognitoService(@Value("${aws.region}") String region) {
        this.cognitoClient = CognitoIdentityProviderClient.builder()
                .region(Region.of(region))
                .build();
    }

    public void setOrganizationId(String username, String organizationId){
        cognitoClient.adminUpdateUserAttributes(AdminUpdateUserAttributesRequest.builder()
                .userPoolId(userPoolId)
                .username(username)
                .userAttributes(AttributeType.builder()
                        .name("custom:organizationId")
                        .value(organizationId)
                        .build())
                .build());
    }

}
