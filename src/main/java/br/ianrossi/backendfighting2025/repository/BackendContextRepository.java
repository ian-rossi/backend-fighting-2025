package br.ianrossi.backendfighting2025.repository;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import br.ianrossi.backendfighting2025.model.BackendContext;
import br.ianrossi.backendfighting2025.model.EndpointContext;
import br.ianrossi.backendfighting2025.model.EndpointType;
import jakarta.inject.Singleton;

@Singleton
public class BackendContextRepository {

	private static final BackendContext CONTEXT = new BackendContext(
		new AtomicReference<>(), 
		new AtomicBoolean(false),
		new EndpointContext(
			URI.create(System.getenv("DEFAULT_PAYMENT_PROCESSOR_URL")),
			EndpointType.DEFAULT, 
			BigDecimal.valueOf(0.05f), 
			new AtomicReference<>(),
			new AtomicBoolean(false), 
			new AtomicInteger(0), 
			new AtomicReference<>(), 
			new AtomicReference<>(), 
			new AtomicBoolean(Boolean.parseBoolean(System.getenv("SHOULD_EXECUTE_DEFAULT_CHECK")))
		), 
		new EndpointContext(
			URI.create(System.getenv("FALLBACK_PAYMENT_PROCESSOR_URL")), 
			EndpointType.FALLBACK, 
			BigDecimal.valueOf(0.15f), 
			new AtomicReference<>(), 
			new AtomicBoolean(false), 
			new AtomicInteger(0), 
			new AtomicReference<>(), 
			new AtomicReference<>(), 
			new AtomicBoolean(Boolean.parseBoolean(System.getenv("SHOULD_EXECUTE_FALLBACK_CHECK")))
		), 
		Boolean.parseBoolean(System.getenv("DIVIDE_RETRY_DELAY_IS_MS_BY_HALF")), 
		new AtomicBoolean(true), 
		List.of(100, 200, 500, 1000), 
		new LinkedBlockingDeque<>(),
		Boolean.parseBoolean(System.getenv("SHOULD_SEND_POST_PAYMENTS_API_FAILS_TO_OTHER_INSTANCE")),
		new AtomicReference<>()
	);

	public BackendContext get() {
		return CONTEXT;
	}
	
}
