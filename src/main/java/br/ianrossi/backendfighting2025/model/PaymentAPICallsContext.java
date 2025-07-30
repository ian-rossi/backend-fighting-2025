package br.ianrossi.backendfighting2025.model;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

public record PaymentAPICallsContext(
	Iterator<PaymentsAPICallContext> contexts,
	Integer[] postHealthcheckInput,
	AtomicBoolean isThereSomeSuccessToSend,
	AtomicBoolean isThereSomeFailureToSend
) {}
