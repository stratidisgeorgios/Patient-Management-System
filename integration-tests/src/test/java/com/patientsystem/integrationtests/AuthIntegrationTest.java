package com.patientsystem.integrationtests;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
public class AuthIntegrationTest {

    @BeforeAll
    static void setUp(){
        RestAssured.baseURI = "http://localhost:4004";
    }

    @Test
    public void shouldReturnOKWithValidToken(){ //Arrange //Act //Assert
        String loginPayload = """
        {
            "email": "testuser@test.com",
            "password": "password123"
        }
        """;
        
        Response response = RestAssured.given()
            .contentType("application/json")
            .body(loginPayload)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(200)
            .body("token", notNullValue())
            .extract()
            .response();
        System.out.println("Generated Token: " + response.jsonPath().getString("token"));
    }



    @Test
    public void shouldReturnUnauthorizedOnInvalidLogin(){ //Arrange //Act //Assert
        String loginPayload = """
        {
            "email": "testuser@test.com",
            "password": "wrongpassword"
        }
        """;
        RestAssured.given()
            .contentType("application/json")
            .body(loginPayload)
            .when()
            .post("/api/auth/login")
            .then()
            .statusCode(401);
    }
}
