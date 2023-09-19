package com.example.k8sdemoapplication;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@SpringBootApplication(scanBasePackages = "com.example.k8sdemoapplication")
public class DemoApplication {
    private static final Logger logger = LoggerFactory.getLogger(DemoApplication.class);
    private Boolean liveness = Boolean.TRUE;

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

    // Getter for liveness status
    public Boolean isLiveness() {
        logger.info("Liveness status: {}", liveness);
        return liveness;
    }

    // Check liveness for k8s applications
    public boolean isLivenessK8sPod(String podIp) {
        String healthCheckUrl = "http://" + podIp + ":8080/liveness";

        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(healthCheckUrl))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = response.statusCode();

            return statusCode == 200; // Assuming 200 means the pod is healthy
        } catch (Exception e) {
            e.printStackTrace(); // Handle exceptions as needed
            return false; // If an error occurs, consider the pod as unhealthy
        }
    }

    // Get initial status as "GOOD" or "BAD"
    public String getInitialStatus() {
        return liveness ? "GOOD" : "BAD";
    }

    // Toggle liveness status
    public void toggleLiveness() {
        this.liveness = !this.liveness;
    }

    // Get hostname
    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            logger.error("Failed to get hostname: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}