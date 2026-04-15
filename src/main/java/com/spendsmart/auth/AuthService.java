/**
 * Auth service application bootstrap class for the SpendSmart platform.
 *
 * <p>This class initializes the Spring Boot runtime, scans all components under
 * the {@code com.spendsmart.auth} package, and registers this service with Eureka
 * through Spring Cloud discovery.
 *
 * <p>Dependencies: Spring Boot autoconfiguration, Spring Cloud Netflix Eureka.
 */
package com.spendsmart.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class AuthService {

    public static void main(String[] args) {
        SpringApplication.run(AuthService.class, args);
    }
}
