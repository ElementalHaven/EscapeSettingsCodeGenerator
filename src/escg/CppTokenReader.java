package escg;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.List;

public class CppTokenReader implements AutoCloseable {
	// equals considered a single token even though <=, >=, and == are a thing
	// hopefully it should never be relevant
	private static final List<Character>	SINGLE_CHAR_TOKENS	= List.of(':', '{', '}', ';', ',', '=');

	private static final List<Character>	SECTION_DELIMITERS	= List.of(';', '{', '}');

	private static final List<String>		PROTECTION_LEVELS	= List.of("public", "private", "protected");

	private final PushbackReader			reader;
	private final StringBuilder				buf					= new StringBuilder();
	private boolean							finishedOnALine		= false;

	public CppTokenReader(Reader reader) {
		this.reader = new PushbackReader(reader, 2);
	}

	private void finishReadingLineComment() throws IOException {
		finishedOnALine = true;
		while(true) {
			int c = reader.read();
			if(c == '\n' || c == -1) return;
			buf.appendCodePoint(c);
		}
	}
	
	private void finishReadingBlockComment() throws IOException {
		int lastChar = 0;
		while(true) {
			int c = reader.read();
			if(c == -1) return;
			// make sure the closing bit is part of the token
			buf.appendCodePoint(c);
			if(c == '/' && lastChar == '*') return;
			lastChar = c;
		}
	}
	
	private void finishReadingParenthesis() throws IOException {
		int toSkip = 0;
		while(true) {
			int c = reader.read();
			if(c == -1) return;
			// make sure the closing bit is part of the token
			buf.appendCodePoint(c);
			if(c == '(') toSkip++;
			// post decrement done after comma check but equality check done before
			if(c == ')' && toSkip-- == 0) return;
		}
	}
	
	// read a normal string
	private void finishReadingString() throws IOException {
		int lastChar = 0;
		while(true) {
			int c = reader.read();
			if(c == -1) return;
			buf.appendCodePoint(c);
			if(c == '"' && lastChar != '\\') return;
			lastChar = c;
		}
	}
	
	// the most basic of raw string support
	// no custom delimiters are considered
	private void finishReadingRawString() throws IOException {
		int lastChar = 0;
		while(true) {
			int c = reader.read();
			if(c == -1) return;
			buf.appendCodePoint(c);
			if(c == '"' && lastChar == ')') return;
			lastChar = c;
		}
	}
	
	public String nextToken() throws IOException {
		buf.setLength(0);
		boolean tokenStarted = false;
		int lastChar = 0;
		while(true) {
			int c = reader.read();
			if(c == -1) break;
			if(Character.isWhitespace(c)) {
				if(tokenStarted) {
					if(lastChar == ':') {
						reader.unread(':');
						buf.setLength(buf.length() - 1);
					}
					break;
				}
			} else if(lastChar == ':' && !Character.isLetter(c)) {
				// putting in spaceless bitfield support even though it'll never be useful to me.
				// whoever it's relevant to better be grateful -Liz (7/30/23)
				reader.unread(c);
				reader.unread(':');
				break;
			} else {
				// special care is put into handling colons
				// as they could be used for namespace names, case labels, or bitfields
				final boolean isSingular = SINGLE_CHAR_TOKENS.contains((char) c);
				if(!tokenStarted && isSingular) {
					// toString(int) added in Java 11
					// if you really insist on reducing dependency to an earlier version,
					// you could probably just cast c to char and be fine
					return Character.toString(c);
				}
				tokenStarted = true;
				// allow ':'s because we could have a namespace name
				if(isSingular && c != ':') {
					reader.unread(c);
					break;
				}
				buf.appendCodePoint(c);
				if(c == '/' && lastChar == '/') {
					finishReadingLineComment();
					break;
				} else if(c == '*' && lastChar == '/') {
					finishReadingBlockComment();
					break;
				} else if(c == '"') {
					if(lastChar == 'R') {
						finishReadingRawString();
					} else {
						finishReadingString();						
					}
					break;
				} else if(c == '(') {
					finishReadingParenthesis();
					break;
				}
				lastChar = c;
			}
		}
		return buf.length() == 0 ? null : buf.toString();
	}
	
	public void readToNextDelimiter(List<String> tokens) throws IOException {
		String lastToken = null;
		while(true) {
			String token = nextToken();
			if(token == null) break;
			
			char c = token.charAt(0);
			if(SECTION_DELIMITERS.contains(c)) break;
			if(c == ':') {
				if(PROTECTION_LEVELS.contains(lastToken)) break;
				
				String firstToken = tokens.get(0);
				if(firstToken.equals("case") || firstToken.equals("default")) break;
			}
			
			lastToken = token;
		}
	}
	
	public void skipLine() throws IOException {
		if(finishedOnALine) return;
		// cheat and reuse existing code
		// it does basically the same thing, only the buffered data is never used
		finishReadingLineComment();
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}
}