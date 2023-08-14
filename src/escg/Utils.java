package escg;

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
		return null;
	}
	
	public static boolean parseBoolean(String value, boolean defaultVal) {
		switch(value.toLowerCase()) {
			case "1":
			case "true":
			case "yes":
			case "y":
			case "t":
				return true;
			case "0":
			case "false":
			case "no":
			case "n":
			case "f":
				return false;
			default:
				return defaultVal;
		}
	}
}