package br.ianrossi.backendfighting2025.repository;

import java.time.Instant;
import java.util.Map;

import br.ianrossi.backendfighting2025.model.EndpointType;
import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.search.AggregateArgs;
import io.quarkus.redis.datasource.search.AggregateArgs.GroupBy;
import io.quarkus.redis.datasource.search.AggregationResponse;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentsSummaryRepository {
	
	private static final AggregateArgs AGGREGATE_ARGS = new AggregateArgs()
		.groupBy(new GroupBy().addProperty("@d").addReduceFunction("COUNT", "c"));
	
	ReactiveRedisDataSource dataSource;
	
	public Uni<AggregationResponse> getPaymentsSummary(final Instant from, final Instant to) {
		final var fromAsNumber = from.toEpochMilli();
		final var toAsNumber = to.toEpochMilli();
		return dataSource.search().ftAggregate(
			"p", String.format("@t:[%d %d]", fromAsNumber, toAsNumber), AGGREGATE_ARGS
		);
	}

	public Uni<Long> sendToPaymentsIndex(final Instant requestedAt, final EndpointType endpointType) {
		final var requestedAtStr = Long.toString(requestedAt.toEpochMilli());
		return dataSource.hash(String.class).hset(
			String.format("p:%s", requestedAtStr), 
			Map.of(
				"d", endpointType.getIsDefault(), 
				"t", requestedAtStr
			)
		);
	}

}
