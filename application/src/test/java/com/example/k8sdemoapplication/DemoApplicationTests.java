package com.example.k8sdemoapplication;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class DemoApplicationTests {

	@Autowired
	private TestRestTemplate template;

	@Test
	public void getInfo() throws Exception {
		ResponseEntity<String> response = template.getForEntity("/actuator/info", String.class);
		assertThat(response.getBody()).contains("k8s-demo-application");
	}
}