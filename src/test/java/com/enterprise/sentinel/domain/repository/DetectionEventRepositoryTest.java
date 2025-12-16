package com.enterprise.sentinel.domain.repository;

import com.enterprise.sentinel.domain.model.DetectionEvent;
import com.enterprise.sentinel.domain.model.Video;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DetectionEventRepositoryTest {

	@Container
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
			.withDatabaseName("sentinel_test").withUsername("test").withPassword("test");

	@DynamicPropertySource
	static void configureProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
	}

	@Autowired
	private DetectionEventRepository repository;

	@Autowired
	private TestEntityManager entityManager;

	@Test
	@DisplayName("Should find events by JSONB class property")
	void findObjectsByClass() {
		// 1. Setup Video
		Video video = Video.builder().originalFilename("test_cam.mp4").storagePath("/enc/path").checksum("sha256_dummy")
				.build();
		entityManager.persist(video);

		// 2. Setup Detections (Person and Car)
		DetectionEvent personEvent = DetectionEvent.builder().video(video).timestampMs(1000L)
				.inferenceData(Map.of("class", "person", "conf", 0.99)).build();

		DetectionEvent carEvent = DetectionEvent.builder().video(video).timestampMs(2000L)
				.inferenceData(Map.of("class", "car", "conf", 0.85)).build();

		entityManager.persist(personEvent);
		entityManager.persist(carEvent);
		entityManager.flush();

		// 3. Test Native Query
		var people = repository.findObjectsByClass(video.getId(), "person");
		var cars = repository.findObjectsByClass(video.getId(), "car");

		// 4. Assertions
		assertThat(people).hasSize(1);
		assertThat(people.get(0).getInferenceData().get("class")).isEqualTo("person");

		assertThat(cars).hasSize(1);
		assertThat(cars.get(0).getInferenceData().get("class")).isEqualTo("car");
	}
}