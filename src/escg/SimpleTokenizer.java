package escg;

public class SimpleTokenizer {
	// # = skip preprocessor. don't do conditional structs
	// * = don't support block comments or pointers
	// () = don't support methods
	// [] = don't support arrays
	private static final char[] BAD_CHARS = { '#', '(', ')', '[', ']', '*' };
	
	public static class TokenInfo {
		String value;
		boolean isClass;
	}

	private String line;
	
	void setLine(String line) {
		this.line = line.trim();
	}
	
	String getRemaining() {
		return line;
	}
	
	boolean isEmpty() {
		return line.isEmpty();
	}
	
	boolean startsWithComment() {
		int commentStart = line.indexOf("//");
		if(commentStart > 0) {
			line = line.substring(0, commentStart);
		}
		return commentStart == 0;
	}
	
	boolean hasBadCharacters() {
		int len = line.length();
		for(int i = 0; i < len; i++) {
			char c = line.charAt(i);
			for(char badChar : BAD_CHARS) {
				if(c == badChar) return true;
			}
		}
		return false;
	}
	
	private void trimEnumTrailing(int start) {
		int idx = Math.max(line.indexOf(',', start) + 1, start);
		line = line.substring(idx).stripLeading();
	}
	
	private int nextBreakingCharacter(int start, char... badChars) {
		int len = line.length();
		for(int i = start; i < len; i++) {
			char c = line.charAt(i);
			if(Character.isWhitespace(c)) return i;
			for(char badChar : badChars) {
				if(c == badChar) return i;
			}
		}
		return len;
	}
	
	String readEnumMember() {
		// read everything up until we reach an equals sign, whitespace character, or comma
		int i = 0;
		int len = line.length();
		for(; i < len; i++) {
			char c = line.charAt(i);
			if(Character.isWhitespace(c) || c == '=' || c == ',') break;
		}
		if(i == 0) return null;
		String ret = line.substring(0, i);
		trimEnumTrailing(i);
		return ret;
	}
	
	CppEnum getEnum() {
		if(!line.startsWith("enum ")) return null;

		CppEnum enm = new CppEnum();
		line = line.substring(5).stripLeading();
		if(line.startsWith("class ")) {
			enm.isClass = true;
			line = line.substring(6).stripLeading();
		}
		
		int breaking = nextBreakingCharacter(0, ':', '{');
		String name = line.substring(0, breaking);
		enm.name = name;

		CppEnum.BY_NAME.put(name, enm);
		return enm;
	}
	
	String nextSimpleToken() {
		return nextComplexToken();
	}
	
	private String nextComplexToken(char... badChars) {
		int breaking = nextBreakingCharacter(0, badChars);
		String token = line.substring(0, breaking);
		line = line.substring(breaking).stripLeading();
		return token;
	}
	
	void finishParsingMember(SettingOrGroup item) {
		String name = nextComplexToken(':', '=', ';');
		item.setName(name);
		if(line.isEmpty() || item.getClass() != Setting.class) return;
		char next = line.charAt(0);
		if(next == ':') { // bitfield
			line = line.substring(1).stripLeading();
			// discard bitfield bits
			nextComplexToken('=', ';');
			if(line.isEmpty()) return;
			next = line.charAt(0);
		}
		if(next == ';') return;

		int idx = line.lastIndexOf(';');
		String defVal =  line.substring(1, idx).trim();
		((Setting) item).defaultValue = defVal;
	}
	
	void finishParsingNamedClass(Group group) {
		String typeName = nextComplexToken(':', '{');
		if(!typeName.isEmpty()) {
			group.typeName = typeName;
			Group.BY_NAME.put(typeName, group);
		}
		// we only care about the below for determining parent
		if(line.isEmpty() || line.charAt(0) != ':') return;
		
		while(true) {
			line = line.substring(1).stripLeading();
			
			boolean parentVisible = false;
			boolean namedAlready = false;
			String parentName = nextComplexToken(',', '{');
			switch(parentName) {
				case "public":
					parentVisible = true;
					break;
				case "protected":
				case "private":
					parentVisible = false;
					break;
				default:
					// default is private so parent is of no use
					namedAlready = true;
					break;
			}
			if(!namedAlready) {
				parentName = nextComplexToken(',', '{');
			}
			if(parentVisible) {
				Group parent = Group.BY_NAME.get(parentName);
				if(parent == null) {
					System.err.println("Can't add parent type " + parentName +
							" because it wasn't defined previously in this file");
				} else {
					group.parentTypes.add(parent);
				}
			}
			if(line.isEmpty() || line.charAt(0) == '{') break;
		}
	}
}