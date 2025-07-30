package br.ianrossi.backendfighting2025.service;

import br.ianrossi.backendfighting2025.repository.BackendContextRepository;
import jakarta.inject.Singleton;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.experimental.FieldDefaults;

@Singleton
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BackendContextService {

	@Delegate
	BackendContextRepository repository;
	
}
