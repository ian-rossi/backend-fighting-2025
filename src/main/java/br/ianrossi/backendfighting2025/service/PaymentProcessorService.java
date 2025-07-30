package br.ianrossi.backendfighting2025.service;

import org.eclipse.microprofile.rest.client.ext.ResponseExceptionMapper;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import br.ianrossi.backendfighting2025.model.GetServiceHealthOutput;
import br.ianrossi.backendfighting2025.model.PostPaymentProcessorInput;
import io.quarkus.rest.client.reactive.ClientExceptionMapper;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.handler.HttpException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/payments")
@RegisterRestClient
public interface PaymentProcessorService extends ResponseExceptionMapper<Throwable> {

	@GET
	@Path("/service-health")
	@Produces(MediaType.APPLICATION_JSON)
	Uni<GetServiceHealthOutput> getServiceHealth();

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	Uni<Void> postPayments(final PostPaymentProcessorInput input);

	@ClientExceptionMapper
	static RuntimeException toException(final Response response) {
		final var httpStatusCode = response.getStatus();
		return switch (httpStatusCode) {
			case 500 -> new InternalServerErrorException();
			case 422 -> new HttpException(422);
			default -> null;
		};
	}
}
