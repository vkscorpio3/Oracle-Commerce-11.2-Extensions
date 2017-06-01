package com.conbon.atg.extensions.dynamo.admin;

import static atg.core.util.StringUtils.isBlank;
import static atg.core.util.StringUtils.isNumericOnly;
import static java.lang.Integer.parseInt;

public enum TypeToken {
	INTEGER(0), DOUBLE(1), FLOAT(2), LONG(3), STRING(4);

	private int tokenCode;

	private TypeToken(int tokenCode) {
		this.tokenCode = tokenCode;
	}

	public int getTokenCode() {
		return tokenCode;
	}

	public static TypeToken valueOfCodeString(String code) {
		if (isBlank(code)) {
			throw new NullPointerException("Invalid NULL/BLANK value for type token code");
		}
		if (!isNumericOnly(code)) {
			throw new IllegalArgumentException("Not numeric value of type token code");
		}

		int codeNumber = parseInt(code);
		for (TypeToken status : values()) {
			if (status.getTokenCode() == codeNumber) {
				return status;
			}
		}
		throw new IllegalArgumentException("No enum constant com.crc.crcfulfilment.orders.transmission.manual.TypeToken." + code);
	}
}
