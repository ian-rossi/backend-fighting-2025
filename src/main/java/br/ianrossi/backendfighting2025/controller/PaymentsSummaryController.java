package br.ianrossi.backendfighting2025.controller;

import java.time.Instant;

import org.jboss.resteasy.reactive.RestQuery;

import br.ianrossi.backendfighting2025.model.GetPaymentsSummaryOutput;
import br.ianrossi.backendfighting2025.service.PaymentsSummaryService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Path("/payments-summary")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentsSummaryController {
	
	PaymentsSummaryService service;
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<GetPaymentsSummaryOutput> getPaymentsSummary(
		@RestQuery final Instant from, @RestQuery final Instant to
	) {
		return service.getPaymentsSummary(from, to);
	}
	
}
