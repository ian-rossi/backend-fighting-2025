package br.ianrossi.backendfighting2025.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public record EndpointContext(
	URI baseURI, 
	EndpointType endpointType, 
	BigDecimal tax,
	AtomicReference<BigDecimal> estimatedProfitPerSecond, 
	AtomicBoolean failing, 
	AtomicInteger minResponseTime,
	AtomicReference<RetryTimeScaleFactor> retryTimeScaleFactor, 
	AtomicReference<Instant> nextRetryScheduled,
	AtomicBoolean shouldExecuteHealthcheck
) {
	
	private static final BigDecimal A_SECOND_AS_MILLIS = new BigDecimal(Duration.ofSeconds(1).toMillis());
	
	public void setFailing(
			final boolean divideRetryDelayInMsByHalf, 
			final List<Integer> nextRetryDelayTableInMs, 
			final boolean failing
	) {
		if (failing) {
			onEndpointFail(divideRetryDelayInMsByHalf, nextRetryDelayTableInMs);
		} else {
			onEndpointSuccess();
		}
	}

	public void onEndpointSuccess() {
		failing.set(false);
		retryTimeScaleFactor.set(null);
		nextRetryScheduled.set(null);
	}

	public void onEndpointFail(
		final boolean divideRetryDelayInMsByHalf, final List<Integer> nextRetryDelayTableInMs
	) {
		failing.set(true);
		onChangeRetryTimeScaleFactor(
			divideRetryDelayInMsByHalf, 
			nextRetryDelayTableInMs, 
			RetryTimeScaleFactor.ONE
		);
	}

	public void increaseRetryTimeScaleFactor(
		final boolean divideRetryDelayInMsByHalf, final List<Integer> nextRetryDelayTableInMs
	) {
		final var factor = retryTimeScaleFactor.get();
		final var nextFactor = RetryTimeScaleFactor.FOUR.equals(factor) ? 
				RetryTimeScaleFactor.ONE : 
				RetryTimeScaleFactor.forNumber(factor.ordinal());
		onChangeRetryTimeScaleFactor(
			divideRetryDelayInMsByHalf, nextRetryDelayTableInMs, nextFactor
		);
	}

	public void onChangeRetryTimeScaleFactor(
		final boolean divideRetryDelayInMsByHalf, 
		final List<Integer> nextRetryDelayTableInMs,
		final RetryTimeScaleFactor factor
	) {
		retryTimeScaleFactor.set(factor);
		final var nextRetryDelayInMs = nextRetryDelayTableInMs.get(factor.ordinal());
		final var nextRetryDelayInMsTreated = divideRetryDelayInMsByHalf ? 
			nextRetryDelayInMs / 2 : 
			nextRetryDelayInMs;
		nextRetryScheduled.set(Instant.now().plusMillis(nextRetryDelayInMsTreated));
	}

	public void onChangeAmount(final BigDecimal amount) {
		calculateEstimatedProfitPerSecond(amount);	
	}

	public void onChangeMinResponseTime(final BigDecimal amount, final int minResponseTime) {
		this.minResponseTime.set(minResponseTime);
		calculateEstimatedProfitPerSecond(amount);
	}
	
	private void calculateEstimatedProfitPerSecond(final BigDecimal amount) {
		if (amount != null) {
			final var timeMultiplier = this.minResponseTime.get() == 0 ? 
				A_SECOND_AS_MILLIS :
				A_SECOND_AS_MILLIS.divide(new BigDecimal(this.minResponseTime.get()), 2, RoundingMode.UP); 
			estimatedProfitPerSecond.set(
				amount.multiply(timeMultiplier).multiply(BigDecimal.ONE.subtract(tax))
			);			
		}
	}
}
