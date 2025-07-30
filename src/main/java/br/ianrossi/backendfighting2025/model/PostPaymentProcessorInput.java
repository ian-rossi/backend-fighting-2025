package br.ianrossi.backendfighting2025.model;

import java.math.BigDecimal;

public record PostPaymentProcessorInput(String correlationId, BigDecimal amount, String requestedAt) {}
