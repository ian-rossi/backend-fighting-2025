package br.ianrossi.backendfighting2025.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public enum EndpointType {
	DEFAULT("y"),
	FALLBACK("n");

	String isDefault;

}
