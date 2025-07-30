package br.ianrossi.backendfighting2025.model;

import java.math.BigDecimal;

public record PostPaymentsInput(String correlationId, BigDecimal amount) {}
