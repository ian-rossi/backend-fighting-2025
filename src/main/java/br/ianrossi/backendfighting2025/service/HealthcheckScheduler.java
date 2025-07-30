package br.ianrossi.backendfighting2025.service;

import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HealthcheckScheduler {
	
	HealthcheckService service;
	
	@Scheduled(every = "5s")
	void updateServiceHealthStatuses() {
		service.updateServiceHealthStatuses();
	}
	
}
