package br.ianrossi.backendfighting2025.model;

public enum RetryTimeScaleFactor {
	ONE, TWO, THREE, FOUR;
	
	public static RetryTimeScaleFactor forNumber(final int ordinal) {
		return RetryTimeScaleFactor.values()[ordinal];
	}

}
