package escg;

import java.awt.Color;

public class Utils {
	public static String toSnakeCase(String token) {
		StringBuilder sb = new StringBuilder();
		final int len = token.length();
		// start out acting as if there was a previous capital
		// so that we don't insert a starting _ for TitleCase
		boolean lastWasCaps = true;
		for(int i = 0; i < len; i++) {
			char c = token.charAt(i);
			char lower = Character.toLowerCase(c);
			if(c != lower) {
				if(!lastWasCaps) {
					sb.append('_');
				}
				lastWasCaps = true;
			}
			
			sb.append(lower);
		}
		return sb.toString();
	}
	
	public static String snakeToFriendly(String token) {
		StringBuilder sb = new StringBuilder();
		final int len = token.length();
		// start out acting as if there was a previous space
		// so that we capitalize the first item
		boolean lastWasSpace = true;
		for(int i = 0; i < len; i++) {
			char c = token.charAt(i);
			if(c == '_') {
				c = ' ';
				lastWasSpace = true;
			} else {
				if(lastWasSpace) {
					c = Character.toUpperCase(c);
				}
				lastWasSpace = false;
			}			
			sb.append(c);
		}
		return sb.toString();
	}
	
	public static Class<?> getJavaType(String cppType) {
		if(cppType.equals("bool")) return Boolean.class;
		if(cppType.equals("float")) return Float.class;
		if(cppType.equals("double")) return Double.class;
		// char * not currently supported
		if(cppType.contains("string")) return String.class;
		if(cppType.contains("int")) {
			// unsigned disregarded
			
			// support stdint types.
			if(cppType.contains("64")) return Long.class;
			if(cppType.contains("16")) return Short.class;
			if(cppType.contains("8")) return Byte.class;
			
			return Integer.class;
		}
		if(cppType.contains("vec3") || cppType.contains("vec4")) {
			return Color.class;
		}
		return null;
	}
	
	public static boolean parseBoolean(String value, boolean defaultVal) {
		return parseBoolean(value, (Boolean) defaultVal);
	}
	
	// exists to support null(aka undefined/error) as a third possibility
	public static Boolean parseBoolean(String value, Boolean defaultVal) {
		switch(value.toLowerCase()) {
			case "1":
			case "true":
			case "yes":
			case "y":
			case "t":
			case "on":
				return true;
			case "0":
			case "false":
			case "no":
			case "n":
			case "f":
			case "off":
				return false;
			default:
				return defaultVal;
		}
	}
	
	public static boolean isCppFile(String name) {
		int idx = name.lastIndexOf('.');
		if(idx == -1) return false;
		String ext = name.substring(idx + 1).toLowerCase();
		//* not supporting anything other than headers atm
		// the files need to be includable by the generated files
		// and modules are a bit too complicated for this simple generator
		return "h".equals(ext) || "hpp".equals(ext);
		/*/
		switch(ext) {
			case "h":
			case "hpp":
			case "cpp":
			case "c":
			case "cc":
			case "inl":
			case "ixx":
				return true;
		}
		return false;
		//*/
	}
}