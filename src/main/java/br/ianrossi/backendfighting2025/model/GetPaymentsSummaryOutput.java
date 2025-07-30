package br.ianrossi.backendfighting2025.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public record GetPaymentsSummaryOutput(
	@JsonProperty("default") // Had to do this, because 'default' keyword is reserved.
	GetPaymentsSummaryByEndpointOutput defaultt, 
	GetPaymentsSummaryByEndpointOutput fallback
) {}
