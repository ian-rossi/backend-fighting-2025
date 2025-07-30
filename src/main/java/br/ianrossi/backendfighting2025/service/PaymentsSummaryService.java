package br.ianrossi.backendfighting2025.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import br.ianrossi.backendfighting2025.model.EndpointType;
import br.ianrossi.backendfighting2025.model.GetPaymentsSummaryByEndpointOutput;
import br.ianrossi.backendfighting2025.model.GetPaymentsSummaryOutput;
import br.ianrossi.backendfighting2025.repository.PaymentsSummaryRepository;
import io.quarkus.redis.datasource.search.AggregateDocument;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentsSummaryService {

	BackendContextService service;
	PaymentsSummaryRepository repository;

	public Uni<GetPaymentsSummaryOutput> getPaymentsSummary(final Instant from, final Instant to) {
		final var actualFrom = from != null ? from : Instant.MIN;
		final var actualTo = to != null ? to : Instant.MAX;
		return repository.getPaymentsSummary(actualFrom, actualTo).map(response -> {
			final var documents = response.documents();
			final var defaultSummary = getPaymentsSummaryByEndpoint(EndpointType.DEFAULT, documents);
			final var fallbackSummary = getPaymentsSummaryByEndpoint(EndpointType.FALLBACK, documents);
			return new GetPaymentsSummaryOutput(defaultSummary, fallbackSummary);
		});
	}

	private GetPaymentsSummaryByEndpointOutput getPaymentsSummaryByEndpoint(
		final EndpointType endpointType, final List<AggregateDocument> documents
	) {
		documents.forEach(document -> document.properties().forEach((key, value) -> {
			System.out.println("Key: " + key);
			System.out.println("Value name: " + value.name());
			System.out.println("Value: " + value.unwrap().toString());
		}));		
		final var isDefault = endpointType.getIsDefault();
		return documents.stream().filter(document -> {
			final var isDefaultProperty = document.property("d");
			return isDefaultProperty != null && isDefaultProperty.asString().equals(isDefault); 
		}).findFirst().map(doc -> {
			final var count = doc.property("c").asInteger();
			return new GetPaymentsSummaryByEndpointOutput(
				count, service.get().amount().get().multiply(new BigDecimal(count))
			);
		}).orElseGet(GetPaymentsSummaryByEndpointOutput::getEmptyInstance);
	}
	
	public Uni<Long> sendToPaymentsIndex(final Instant requestedAt, final EndpointType endpointType) {
		return repository.sendToPaymentsIndex(requestedAt, endpointType);
	}

}
