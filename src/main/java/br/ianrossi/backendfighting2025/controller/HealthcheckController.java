package br.ianrossi.backendfighting2025.controller;

import br.ianrossi.backendfighting2025.service.HealthcheckService;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Path("/h")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HealthcheckController {

	HealthcheckService service;

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Uni<Void> postPayments(final Integer[] input) {
		return Uni.createFrom().item(() -> service.postHealthcheck(input));
	}

}
