package com.minibank.comptes;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Fournit aux tests un PostgreSQL 17 réel et jetable, démarré par Testcontainers.
 * {@code @ServiceConnection} branche automatiquement la datasource Spring sur ce conteneur :
 * les tests ne dépendent ni de la base du poste, ni d'un service de la CI.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	@Bean
	@ServiceConnection
	PostgreSQLContainer postgresContainer() {
		// Même version majeure qu'en local (infra/docker-compose.yml) : on teste sur le vrai moteur
		return new PostgreSQLContainer(DockerImageName.parse("postgres:17"));
	}

}
