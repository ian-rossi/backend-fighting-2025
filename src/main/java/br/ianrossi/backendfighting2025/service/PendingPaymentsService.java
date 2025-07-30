package br.ianrossi.backendfighting2025.service;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import br.ianrossi.backendfighting2025.model.BackendContext;
import br.ianrossi.backendfighting2025.model.PaymentAPICallsContext;
import br.ianrossi.backendfighting2025.model.PaymentsAPICallContext;
import br.ianrossi.backendfighting2025.utils.PostHealthcheckInputUtils;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PendingPaymentsService {
	
	private static final int MAX_MS_NECESSARY_TO_COMPLETE_API_REQUEST = 10;

	BackendContextService service;
	PaymentsSummaryService summaryService;
	PaymentProcessorClientService clientService;
	OtherInstanceClientService otherInstanceClientService;
	
	public void processPendingPayment() {
		final var context = service.get();
		final var availableEndpoints = context.getAvailableEndpointsOrderedByTheBest();
		if (availableEndpoints.isEmpty()) {
			return;
		} 
		final var correlationId = context.pollLastItemOfPendingPaymentList();
		if (correlationId == null) {
			return;
		}
		final var paymentAPICallsContext = new PaymentAPICallsContext(
			availableEndpoints.stream().map(
				endpoint -> new PaymentsAPICallContext(
					endpoint,
					requestedAt -> clientService.postPayments(
						endpoint.baseURI(), correlationId, requestedAt
					),
					new AtomicReference<>(),
					PostHealthcheckInputUtils.getStartIndex(endpoint.endpointType())
				)
			).iterator(), 
			new Integer[] { null, null, null, null, null, null }, 
			new AtomicBoolean(false),
			new AtomicBoolean(false)
		);
		final var paymentAPICallContexts = paymentAPICallsContext.contexts();
		final var paymentAPICallContext = paymentAPICallContexts.next();
		final var requestedAt = Instant.now();
		paymentAPICallContext.requestedAt().set(requestedAt);
		paymentAPICallContext.paymentsAPICall().apply(requestedAt)
			.map(ignored -> paymentAPICallContext)
			.onFailure(InternalServerErrorException.class)
			.recoverWithUni(t -> {
				System.out.println("Contexto com falha: " + paymentAPICallContext);
				t.printStackTrace();
				System.out.println("Failure on call " + paymentAPICallContext.endpointContext().baseURI() + " API: " + t);
				onFailure(context, paymentAPICallsContext, paymentAPICallContext);
				if (!paymentAPICallContexts.hasNext()) {
					context.pushToPendingPaymentList(correlationId);
					return Uni.createFrom().failure(t);
				}
				final var nextPaymentAPICallContext = paymentAPICallContexts.next();
				final var nextRequestedAt = Instant.now();
				nextPaymentAPICallContext.requestedAt().set(nextRequestedAt);
				return nextPaymentAPICallContext.paymentsAPICall().apply(nextRequestedAt)
					.map(ignored -> nextPaymentAPICallContext)
					.onFailure(InternalServerErrorException.class)
					.call(thr -> {
						thr.printStackTrace();
						System.out.println("Failure on call " + nextPaymentAPICallContext.endpointContext().baseURI() + " API: " + thr);
						onFailure(context, paymentAPICallsContext, nextPaymentAPICallContext);
						context.pushToPendingPaymentList(correlationId);
						return Uni.createFrom().failure(thr);
					}).onFailure().call(th -> {
						th.printStackTrace();
						System.out.println("Failure on call " + nextPaymentAPICallContext.endpointContext().baseURI() + " API: " + th);
						onFailure(context, paymentAPICallsContext, nextPaymentAPICallContext);
						return Uni.createFrom().failure(th);
					});
			}).chain(ctx -> {
				System.out.println("POST payments API call context: " + ctx);
				checkIfMinResponseTimeChanged(context, paymentAPICallsContext, ctx);
				final var endpointContext = ctx.endpointContext();
				final var healthcheckInput = paymentAPICallsContext.postHealthcheckInput();
				final var startIndex = ctx.healthcheckInputStartIndex();
				if (endpointContext.failing().get()) {
					endpointContext.onEndpointSuccess();
					healthcheckInput[startIndex] = 0;
					paymentAPICallsContext.isThereSomeSuccessToSend().set(true);
				}
				return summaryService.sendToPaymentsIndex(
					ctx.requestedAt().get(), ctx.endpointContext().endpointType()
				).onItemOrFailure().call((i, t) -> {
					if (t != null) {
						t.printStackTrace();
						System.out.println("Error on send to payments index: " + t);
						return Uni.createFrom().failure(t);
					}
					System.out.println("Successfully sent to payments index");
					return Uni.createFrom().item(i);
				});
			}).eventually(() -> {
				System.out.println("Pending payment healthcheck process");
				if (
					paymentAPICallsContext.isThereSomeSuccessToSend().get() || (
						paymentAPICallsContext.isThereSomeFailureToSend().get() && 
						context.shouldSendPostPaymentsAPIFailsToOtherInstance()
					)
				) {
					final var requestStarted = System.nanoTime(); 
					return otherInstanceClientService.postHealthcheck(
						paymentAPICallsContext.postHealthcheckInput()
					).map(ignored -> {
						final var requesteEnded = System.nanoTime();
						System.out.println("ms necessary to run PostHealthcheck: " + ((requesteEnded - requestStarted) / 1_000_000));
						return ignored;
					});
				}
				return Uni.createFrom().voidItem();
			}).subscribe().with(
				ignored -> System.out.println("Finished pending payments"),
				Throwable::printStackTrace
		    );
	}

	private static void onFailure(
		final BackendContext backendContext,
		final PaymentAPICallsContext paymentsAPICallContext, 
		final PaymentsAPICallContext endpointPaymentsAPICallContext
	) {
		final var endpointContext = endpointPaymentsAPICallContext.endpointContext();
		checkIfMinResponseTimeChanged(
			backendContext, 
			paymentsAPICallContext, 
			endpointPaymentsAPICallContext
		);
		final var healthcheckInput = paymentsAPICallContext.postHealthcheckInput();
		final var startIndex = endpointPaymentsAPICallContext.healthcheckInputStartIndex();
		if (!endpointContext.failing().get()) {
			endpointContext.onEndpointFail(
				backendContext.divideRetryDelayInMsByHalf(), 
				backendContext.nextRetryDelayTableInMs()
			);
			healthcheckInput[startIndex] = 1;
		} else {
			endpointContext.increaseRetryTimeScaleFactor(
				backendContext.divideRetryDelayInMsByHalf(), 
				backendContext.nextRetryDelayTableInMs()
			);
			healthcheckInput[startIndex + 2] = 
				endpointContext.retryTimeScaleFactor().get().ordinal();
		}
		paymentsAPICallContext.isThereSomeFailureToSend().set(true);
	}

	private static void checkIfMinResponseTimeChanged(
		final BackendContext backendContext,
		final PaymentAPICallsContext paymentsAPICallContext, 
		final PaymentsAPICallContext endpointPaymentsAPICallContext
	) {
		if (!backendContext.itAlreadyPassedTenSecondsFromWhenFirstPostPaymentsRequestHappened()) {
			return;
		}
		final var timeToRunRequestInMs = System.currentTimeMillis() - 
			endpointPaymentsAPICallContext.requestedAt().get().toEpochMilli();		
		final var estimatedMinResponseTime = (
			(int) timeToRunRequestInMs / MAX_MS_NECESSARY_TO_COMPLETE_API_REQUEST
		) * MAX_MS_NECESSARY_TO_COMPLETE_API_REQUEST;
		final var endpointContext = endpointPaymentsAPICallContext.endpointContext();
		if (estimatedMinResponseTime != endpointContext.minResponseTime().get()) {
			endpointContext.onChangeMinResponseTime(
				backendContext.amount().get(), estimatedMinResponseTime
			);
			final var healthcheckInput = paymentsAPICallContext.postHealthcheckInput();
			final var startIndex = endpointPaymentsAPICallContext.healthcheckInputStartIndex();
			healthcheckInput[startIndex + 1] = estimatedMinResponseTime;
			paymentsAPICallContext.isThereSomeSuccessToSend().set(true);
		}
	}

}
