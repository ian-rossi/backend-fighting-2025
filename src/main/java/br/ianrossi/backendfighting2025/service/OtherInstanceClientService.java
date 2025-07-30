package br.ianrossi.backendfighting2025.service;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

@Singleton
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtherInstanceClientService {

	@Delegate
	@RestClient
	OtherInstanceService service;

}
