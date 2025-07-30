package br.ianrossi.backendfighting2025.service;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;

@Path("/h")
@RegisterRestClient(configKey = "other-instance")
public interface OtherInstanceService {

	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	Uni<Void> postHealthcheck(final Integer[] input);

}
