package br.ianrossi.backendfighting2025.service;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import br.ianrossi.backendfighting2025.model.GetServiceHealthOutput;
import br.ianrossi.backendfighting2025.model.PostPaymentProcessorInput;
import io.quarkus.rest.client.reactive.QuarkusRestClientBuilder;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentProcessorClientService {

	BackendContextService service;
	
	public Uni<GetServiceHealthOutput> getServiceHealth(final URI baseURI) {
		final var processorService = getProcessorService(baseURI);
		return processorService.getServiceHealth();
	}

	public Uni<Void> postPayments(
		final URI baseURI, final String correlationId, final Instant requestedAt
	) {
		final var processorService = getProcessorService(baseURI);
		final var input = new PostPaymentProcessorInput(
			correlationId, 
			service.get().amount().get(),
			DateTimeFormatter.ISO_INSTANT.format(requestedAt)
		);
		return processorService.postPayments(input);
	}

	private static PaymentProcessorService getProcessorService(final URI baseURI) {
		return QuarkusRestClientBuilder.newBuilder()
			.baseUri(baseURI).build(PaymentProcessorService.class);
	}

}
