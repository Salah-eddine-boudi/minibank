package com.minibank.comptes;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Test de fumée : l'application démarre entièrement (Flyway, JPA en mode validate, Actuator)
 * sur un PostgreSQL réel fourni par Testcontainers.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ComptesServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
