package lucee.runtime.functions.other;

import javax.crypto.spec.SecretKeySpec;

import lucee.runtime.PageContext;
import lucee.runtime.coder.Base64Coder;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.function.Function;

/**
 * Generates a 3DES key
 */
public class Generate3DesKey implements Function {

	private static final long serialVersionUID = 8540648635721649340L;

	public static String call(PageContext pc) throws PageException {

		return GenerateSecretKey.call(pc, "DESede");
	}

	public static String call(PageContext pc, String input) {

		SecretKeySpec keySpec = new SecretKeySpec(input.getBytes(), "DESede");

		return Base64Coder.encode(keySpec.getEncoded());
	}
}
