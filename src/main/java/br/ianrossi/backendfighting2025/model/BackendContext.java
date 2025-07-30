package br.ianrossi.backendfighting2025.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public record BackendContext(
	AtomicReference<BigDecimal> amount,
	AtomicBoolean doesTheApplicationStopped,
	EndpointContext defaultEndpointContext,
	EndpointContext fallbackEndpointContext,
	boolean divideRetryDelayInMsByHalf,
	AtomicBoolean isItTheFirstTimeRunningPaymentsAPI,
	List<Integer> nextRetryDelayTableInMs,
	Deque<String> pendingPaymentsQueue,
	boolean shouldSendPostPaymentsAPIFailsToOtherInstance,
	AtomicReference<Long> whenFirstPostPaymentsRequestHappened
) {
	private static final int FROM_MILLIS_TO_SECONDS = 1_000;
	
	public EndpointContext getEndpoint(final EndpointType endpointType) { 
		return EndpointType.DEFAULT.equals(endpointType) ? 
			defaultEndpointContext : fallbackEndpointContext;
	}
	
	public List<EndpointContext> getEndpoints() { 
		return List.of(defaultEndpointContext, fallbackEndpointContext);
	}

	public List<EndpointContext> getAvailableEndpointsOrderedByTheBest() { 
		final var candidateEndpoints = getEndpoints().stream().filter(
			endpoint -> endpoint.estimatedProfitPerSecond().get() != null && (
				!endpoint.failing().get() || (
					endpoint.nextRetryScheduled().get() != null && 
					Instant.now().compareTo(endpoint.nextRetryScheduled().get()) >= 0
				)
			)
		).toList();
		if (candidateEndpoints.size() == 2) {
			final var defaultEstimatedProfit = defaultEndpointContext.estimatedProfitPerSecond().get();
			final var fallbackEstimatedProfit = fallbackEndpointContext.estimatedProfitPerSecond().get();
			if (defaultEstimatedProfit.compareTo(fallbackEstimatedProfit) >= 0) {
				return List.of(defaultEndpointContext, fallbackEndpointContext);
			}
			return List.of(fallbackEndpointContext, defaultEndpointContext);
		}
		return candidateEndpoints;
	}

	// This function exists bcs the first 10 seconds of initialization of an application is (usually) for warmup.
	public boolean itAlreadyPassedTenSecondsFromWhenFirstPostPaymentsRequestHappened() {
		return (
			System.currentTimeMillis() - whenFirstPostPaymentsRequestHappened.get()
		) / FROM_MILLIS_TO_SECONDS >= 10;
	}

	public void onChangeAmount(final BigDecimal amount) {
		if (amount != null && this.amount.compareAndSet(null, amount)) {
			getEndpoints().forEach(endpoint -> endpoint.onChangeAmount(amount));
		}
		
	}

	public void onChangeMinResponseTime(final EndpointType endpointType, final int minResponseTime) {
		getEndpoint(endpointType).onChangeMinResponseTime(amount.get(), minResponseTime);
	}

	public void increaseRetryTimeScaleFactor(final EndpointType endpointType) {
		getEndpoint(endpointType).increaseRetryTimeScaleFactor(
			divideRetryDelayInMsByHalf, nextRetryDelayTableInMs
		);
	}

	public void pushToPendingPaymentList(final String correlationId) {
		pendingPaymentsQueue.push(correlationId);
	}
	
	public String pollLastItemOfPendingPaymentList() {
		return pendingPaymentsQueue.pollLast();
	}

}
