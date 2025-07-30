package br.ianrossi.backendfighting2025.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import br.ianrossi.backendfighting2025.model.PostPaymentsInput;
import jakarta.inject.Singleton;
import jakarta.ws.rs.InternalServerErrorException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PaymentsService {

	private static final int CORRELATION_ID_VALUE_START_IDX = 18;
	private static final int CORRELATION_ID_VALUE_END_IDX = CORRELATION_ID_VALUE_START_IDX + 36;

	ObjectMapper mapper;
	BackendContextService service;
	
	public Void postPayments(final String jsonAsStr) {
		final var context = service.get();
		String correlationId = null;
		final var isItTheFirstTimeRunningPaymentsAPI = context.isItTheFirstTimeRunningPaymentsAPI();
		if (
			isItTheFirstTimeRunningPaymentsAPI.compareAndSet(true, false) && 
			context.whenFirstPostPaymentsRequestHappened().compareAndSet(
				null, System.currentTimeMillis()
			)
		) {
			final var input = tryParse(jsonAsStr);
			context.onChangeAmount(input.amount());
			correlationId = input.correlationId();
		} else {
			correlationId = jsonAsStr.substring(
				CORRELATION_ID_VALUE_START_IDX, 
				CORRELATION_ID_VALUE_END_IDX
			);
		}
		context.pushToPendingPaymentList(correlationId);
		return null;
	}
	
	private PostPaymentsInput tryParse(final String jsonAsStr) {
		try {
			return mapper.readValue(jsonAsStr, PostPaymentsInput.class);
		} catch (final Exception e) {
			throw new InternalServerErrorException();
		}
	}
}
