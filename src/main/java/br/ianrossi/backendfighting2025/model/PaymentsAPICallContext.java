package br.ianrossi.backendfighting2025.model;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;

public record PaymentsAPICallContext(
	EndpointContext endpointContext,
	Function<Instant, Uni<Void>> paymentsAPICall,
	AtomicReference<Instant> requestedAt,
	int healthcheckInputStartIndex
) {}
