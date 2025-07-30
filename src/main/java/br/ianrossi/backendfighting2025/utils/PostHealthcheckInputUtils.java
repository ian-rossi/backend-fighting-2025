package br.ianrossi.backendfighting2025.utils;

import br.ianrossi.backendfighting2025.model.EndpointType;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PostHealthcheckInputUtils {

	public static int getStartIndex(final EndpointType endpointType) {
		return EndpointType.DEFAULT.equals(endpointType) ? 0 : 3;
	}
	
}
