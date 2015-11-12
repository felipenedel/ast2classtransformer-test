package transformer.utils;

import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Pattern;

public class StringUtils {

	public static URL getClassContainer(Class<?> clazz) throws Exception {
		if (clazz == null) {
			throw new NullPointerException("Cant't parse null Class");
		}

		try {
			while (clazz.isMemberClass() || clazz.isAnonymousClass()) {
				clazz = clazz.getEnclosingClass();
			}

			if (clazz.getProtectionDomain().getCodeSource() == null) {
				// This is a proxy or other dynamically generated class, and has no physical container,
				// so just return null.
				return null;
			}

			String packageRoot;
			try {
				// This is the full path to THIS file, but we need to get the package root.
				String thisClass = clazz.getResource(clazz.getSimpleName() + ".class").toString();
				packageRoot = replaceLastOccurrence(thisClass, Pattern.quote(clazz.getName().replaceAll("\\.", "/") + ".class"), "");

				if (packageRoot.endsWith("!/")) {
					packageRoot = replaceLastOccurrence(packageRoot, "!/", "");
				}
			} catch (Exception e) {
				packageRoot = clazz.getProtectionDomain().getCodeSource().getLocation().toString();
			}

			packageRoot = URLDecoder.decode(packageRoot, "UTF-8");
			return new URL(packageRoot);
		} catch (Exception e) {
			throw new Exception("Unkown error: " + e.getMessage());
		}
	}

	public static String replaceLastOccurrence(String string, String regex, String replacement) {
		if (isBlank(string)) {
			return null;
		}

		while (string.endsWith(regex)) {
			string = string.substring(0, regex.length() - replacement.length());
		}

		return string;
	}

	public static boolean isBlank(String text) {
		if (text != null && text.length() > 0) {
			for (int i = 0, iSize = text.length(); i < iSize; i++) {
				if (text.charAt(i) != ' ') {
					return false;
				}
			}
		}

		return true;
	}
}
