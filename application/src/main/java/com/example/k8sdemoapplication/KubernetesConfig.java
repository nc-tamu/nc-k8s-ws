package com.example.k8sdemoapplication;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KubernetesConfig {

    @Bean
    public KubernetesClient kubernetesClient() {
        KubernetesClient client = new KubernetesClientBuilder().build(); // You might need to customize this configuration.
        return client;
    }
}
