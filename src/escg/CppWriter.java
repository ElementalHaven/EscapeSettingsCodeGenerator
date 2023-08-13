package escg;

public class CppWriter {
	private int				indentAmount;
	protected String		indent				= "\t";
	protected String		lineSeparator		= System.lineSeparator();
	protected boolean		newlineBrackets		= false;
	protected boolean		spaceBeforeBrackets	= true;
	protected boolean		referenceBefore		= true;

	// I'd subclass it if not for the fact that StringBuilder is final
	private StringBuilder	str					= new StringBuilder();

	public CppWriter indent() {
		indentAmount++;
		return this;
	}
	
	public CppWriter unindent() {
		indentAmount--;
		return this;
	}
	
	public CppWriter startLine() {
		for(int i = 0; i < indentAmount; i++) {
			str.append(indent);
		}
		return this;
	}
	
	public CppWriter endLine() {
		str.append(lineSeparator);
		return this;
	}
	
	public CppWriter append(String str) {
		this.str.append(str);
		return this;
	}
	
	public CppWriter append(char c) {
		str.append(c);
		return this;
	}
	
	public CppWriter append(Object obj) {
		str.append(obj);
		return this;
	}
	
	@Override
	public String toString() {
		return str.toString();
	}
	
	public CppWriter clear() {
		str.setLength(0);
		return this;
	}
	
	public CppWriter openBracket() {
		if(newlineBrackets) {
			endLine().startLine();
		} else if(spaceBeforeBrackets) {
			append(' ');
		}
		append('{');
		return this;
	}
	
	public CppWriter reference() {
		return append(referenceBefore ? "& " : " &");
	}
	
	public CppWriter determineLineSeparatorFromReference(String ref) {
		final int refLen = ref.length();
		for(int i = 0; i < refLen; i++) {
			char c = ref.charAt(i);
			if(c == '\r' || c == '\n') {
				char c2 = ref.charAt(i + 1);
				int len = (c2 == '\r' || c2 == '\n') ? 2 : 1; 
				lineSeparator = ref.substring(i, i + len);
				break;
			}
		}
		return this;
	}
	
	public CppWriter determineIndentFromReference(String[] ref) {
		int newline = 0;
		int space = 0;
		int refBefore = 0;
		for(String line : ref) {
			int idx = line.indexOf('&');
			if(idx > 0 && idx < line.length()) {
				boolean spaceBefore = Character.isWhitespace(line.codePointAt(idx - 1));
				boolean spaceAfter = Character.isWhitespace(line.codePointAt(idx + 1));
				if(spaceAfter != spaceBefore) {
					if(spaceAfter) refBefore++;
					else refBefore--;
				}
			}
			
			idx = line.indexOf('{');
			if(idx == -1) continue;
			String trimmed = line.stripLeading();
			if(trimmed.charAt(0) == '{') {
				newline++;
			} else {
				newline--;
				if(Character.isWhitespace(line.codePointAt(idx - 1))) {
					space++;
				} else {
					space--;
				}
			}
		}
		newlineBrackets = newline > 0;
		spaceBeforeBrackets = space > 0;
		referenceBefore = refBefore > 0;
		
		boolean inComment = false;
		for(String line : ref) {
			String trimmed = line.trim();
			if(!trimmed.isEmpty()) {
				boolean lineHadComments = inComment;
				
				int start = 0;
				while(true) {
					int idx = trimmed.indexOf(inComment ? "*/" : "/*", start);
					if(idx == -1) break;
					
					lineHadComments = true;
					inComment = !inComment;
					start = idx + 2;
				}
				
				if(lineHadComments) continue;
				
				int idx = line.indexOf(trimmed);
				if(idx > 0) {
					indent = line.substring(0, idx);
					break;
				}
			}
		}
		return this;
	}
}