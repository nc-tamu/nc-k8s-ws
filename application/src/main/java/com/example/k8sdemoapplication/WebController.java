package com.example.k8sdemoapplication;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/")
public class WebController {
    private static final Logger logger = LoggerFactory.getLogger(WebController.class);

    @Value("${POD_NAMESPACE:default}")
    private String namespace;
    @Value("${IN_K8S:false}")
    private Boolean inK8S;

    @Autowired
    private KubernetesClient kubernetesClient;
    @Autowired
    private DemoApplication demoApplication;

    // Handle GET request for homepage
    @GetMapping
    public String getHostNameAndLiveness(Model model) {
        model.addAttribute("hostname", demoApplication.getHostname());

        // If we are running in Kuberentes, use the API to get pods
        if (inK8S) {
            String statusColor = demoApplication.isLiveness() ? "green" : "red";
            model.addAttribute("statusText", demoApplication.getInitialStatus());
            model.addAttribute("statusColor", statusColor);
            // Get the list of pods in the namespace
            List<Pod> pods = kubernetesClient.pods().inNamespace(namespace).list().getItems();
            logger.info("Number of pods in namespace {}: {}", namespace, pods.size());

            // Map pod names to their respective status (for simplicity, assuming liveness status is stored in DemoApplication)
            Map<String, String> podStatusMap = new HashMap<>();
            for (Pod pod : pods) {
                String podName = pod.getMetadata().getName();
                String podIp = pod.getStatus().getPodIP();
                logger.info("Pod ip in namespace {}", podIp);

                boolean isLivenessK8sPod = demoApplication.isLivenessK8sPod(podIp);
                podStatusMap.put(podName, isLivenessK8sPod ? "GOOD" : "BAD");
            }
            logger.info("Pod Status Map: {}", podStatusMap);

            // Add pod information to the model
            model.addAttribute("podStatusMap", podStatusMap);
        }
        return "index";
    }

    // Handle GET request for liveness status
    @GetMapping("/liveness")
    public ResponseEntity<Void> getLiveness() {
        return demoApplication.isLiveness()
                ? ResponseEntity.ok().build()
                : ResponseEntity.internalServerError().build();
    }

    // Handle POST request to toggle liveness status
    @PostMapping("/liveness")
    public ResponseEntity<String> changeLiveness() {
        demoApplication.toggleLiveness();
        String hostname = demoApplication.getHostname();
        logger.info("Liveness status changed to: {} for hostname: {}", demoApplication.isLiveness(), hostname);
        return ResponseEntity.ok(hostname);
    }
}

