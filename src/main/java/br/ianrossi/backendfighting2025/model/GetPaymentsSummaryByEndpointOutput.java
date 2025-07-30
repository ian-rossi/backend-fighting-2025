package br.ianrossi.backendfighting2025.model;

import java.math.BigDecimal;

public record GetPaymentsSummaryByEndpointOutput(int totalRequests, BigDecimal totalAmount) {
	
	public static GetPaymentsSummaryByEndpointOutput getEmptyInstance() {
		return new GetPaymentsSummaryByEndpointOutput(0, BigDecimal.ZERO);
	}

}
