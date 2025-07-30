package br.ianrossi.backendfighting2025.service;

import java.util.Arrays;
import java.util.List;

import br.ianrossi.backendfighting2025.model.BackendContext;
import br.ianrossi.backendfighting2025.model.RetryTimeScaleFactor;
import br.ianrossi.backendfighting2025.model.ServiceHealthContext;
import br.ianrossi.backendfighting2025.utils.PostHealthcheckInputUtils;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HealthcheckService {

	BackendContextService service;
	PaymentProcessorClientService clientService;
	OtherInstanceClientService otherInstanceClientService;

	public Void postHealthcheck(final Integer[] input) {
		System.out.println("chamou PostHealthcheck JSON");
		final var context = service.get();
		System.out.println("Contexto antes: " + context);
		context.getEndpoints().forEach(endpoint -> {
			final var startIndex = PostHealthcheckInputUtils.getStartIndex(
				endpoint.endpointType()
			);
			final var failing = input[startIndex];
			if (failing != null) {
				endpoint.setFailing(
					context.divideRetryDelayInMsByHalf(), 
					context.nextRetryDelayTableInMs(), 
					failing == 1
				);
			}
			final var minResponseTime = input[startIndex + 1];
			if (minResponseTime != null) {
				endpoint.onChangeMinResponseTime(context.amount().get(), minResponseTime);
			}
			final var retryTimeScaleFactor = input[startIndex + 2];
			if (retryTimeScaleFactor != null) {
				endpoint.onChangeRetryTimeScaleFactor(
					context.divideRetryDelayInMsByHalf(), 
					context.nextRetryDelayTableInMs(), 
					RetryTimeScaleFactor.forNumber(retryTimeScaleFactor)
				);
			}
		});
		return null;
	}

	public void updateServiceHealthStatuses() {
		final var context = service.get();
		final var shouldExecuteDefaultCheck = context.defaultEndpointContext()
			.shouldExecuteHealthcheck();
		final var shouldExecuteFallbackCheck = context.fallbackEndpointContext()
			.shouldExecuteHealthcheck();
		final var shouldExecuteDefaultCheckValue = shouldExecuteDefaultCheck.get();
		System.out.println("Shoudl execute default check: " + shouldExecuteDefaultCheckValue);
		final var shouldExecuteFallbackCheckValue = shouldExecuteFallbackCheck.get();
		System.out.println("Shoudl execute fallback check: " + shouldExecuteFallbackCheckValue);
		if (shouldExecuteDefaultCheckValue && shouldExecuteFallbackCheckValue) {
			Uni.combine().all()
				.unis(
					context.getEndpoints().stream().map(
						endpoint -> clientService.getServiceHealth(endpoint.baseURI())
							.map(output -> new ServiceHealthContext(endpoint, output))
					).toList()
				).withUni(
					ServiceHealthContext.class, outputs -> handleServiceHealthContext(context, outputs)
				).subscribe().with(
					ignored -> System.out.println("Schedule healthcheck runned with success"),
					failure -> failure.printStackTrace()
				);
		}
		shouldExecuteDefaultCheck.set(!shouldExecuteDefaultCheckValue);
		shouldExecuteFallbackCheck.set(!shouldExecuteFallbackCheckValue);
	}

	private Uni<Void> handleServiceHealthContext(
		final BackendContext context, final List<ServiceHealthContext> healthContexts
	) {
		final Integer[] input = { null, null, null, null, null, null };
		var someDataChanged = false;
		for (final var healthContext : healthContexts) {
			final var endpointContext = healthContext.context();
			final var output = healthContext.output();
			final var startIndex = PostHealthcheckInputUtils.getStartIndex(
				endpointContext.endpointType()
			);
			if (endpointContext.failing().get() != output.failing()) {
				someDataChanged = true;
				input[startIndex] = output.failing() ? 1 : 0;
				endpointContext.setFailing(
					output.failing(), 
					context.nextRetryDelayTableInMs(),
					context.divideRetryDelayInMsByHalf()
				);
			}
			if (endpointContext.minResponseTime().get() != output.minResponseTime()) {
				someDataChanged = true;
				input[startIndex + 1] = output.minResponseTime();
				endpointContext.onChangeMinResponseTime(
					context.amount().get(), output.minResponseTime()
				);
			}
		}
		System.out.println("Healthcheck input: " + Arrays.toString(input));
		if (someDataChanged) {
			final var requestStarted = System.nanoTime();
			return otherInstanceClientService.postHealthcheck(input).map(ignored -> {
				System.out.println(String.format("Demorou %d ms", (System.nanoTime() - requestStarted) / 1_000_000));
				return ignored;
			});
		}
		return Uni.createFrom().voidItem();		
	}
}
