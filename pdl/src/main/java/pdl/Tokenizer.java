package pdl;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.math.BigInteger;

class Tokenizer {
	/*
	 * token definitions 
	 */
	static final int EOF = -1, ID = -2, STRING = -3, HERE = -4, INTERP_STRING = -5, NUMBER = -6, L_OR = -7, L_AND = -8,
			EQ = -9, L_SHIFT = -10, R_SHIFT = -11, LE = -12, GE = -13, COLON_EQ = -14, PLUS_EQ = -15, NEQ = -16;
	// internal states
	private static final int S_INIT = 0, S_TRIPLE_QUOTE_STRING = 1, S_SINGLE_QUOTE_STRING = '\'',
			S_DOUBLE_QUOTE_STRING = '\"', S_NUMBER = 2, S_FRAC = 3, S_ID = 4, S_BINARY = 5, S_EXP = 6, S_EXP2 = 7,
			S_LINE_COMMENT = 8, S_BLOCK_COMMENT = 9, S_HEX = 10;

	private String value;
	private int token;
	private int line;
	private final int[] col;

	private final PushbackReader in;

	Tokenizer(Reader in) {
		col = new int[16];
		this.in = new PushbackReader(in, col.length);
	}

	int nextToken() throws IOException {
		StringBuilder buffer = new StringBuilder();
		int state = S_INIT;
		while (true) {
			int ch = read();
			switch (state) {
			case S_INIT:
				if (ch == -1) {
					value = null;
					return token = EOF;
				}
				if (Character.isWhitespace(ch)) {
					break;
				}
				if (ch == '#') {
					state = S_LINE_COMMENT;
					break;
				}
				if (ch == '/') {
					ch = read();
					if (ch == '*') {
						state = S_BLOCK_COMMENT;
						break;
					} else if (ch == '/') {
						state = S_LINE_COMMENT;
						break;
					}
					unread(ch);
					ch = '/';
				}
				if (ch == '\"') {
					if (isTripleQuote(ch)) {
						state = S_TRIPLE_QUOTE_STRING;
						token = STRING;
					} else {
						state = S_DOUBLE_QUOTE_STRING;
						token = INTERP_STRING;
					}
					break;
				}
				if (ch == '\'') {
					state = S_SINGLE_QUOTE_STRING;
					token = STRING;
					break;
				}
				if (ch >= '0' && ch <= '9') {
					state = S_NUMBER;
					buffer.append((char) ch);
					int ch2 = read();
					if (ch2 == 'x' || ch2 == 'X') {
						buffer.append((char) ch2);
						state = S_HEX;
					} else if (ch2 == 'b') {
						state = S_BINARY;
					} else {
						unread(ch2);
					}
					break;
				}
				if (ch == '-' || ch == '+') {
					int ch2 = read();
					if ((ch2 >= '0' && ch2 <= '9') || ch == '.') {
						buffer.append((char) ch);
						buffer.append((char) ch2);
						state = ch == '.' ? S_FRAC : S_NUMBER;
						break;
					}
					unread(ch2);
				}
				if (ch == '+' || ch == ':' || ch == '!') {
					int ch2 = read();
					if (ch2 == '=') {
						switch (ch) {
						case '+':
							return token = PLUS_EQ;
						case ':':
							return token = COLON_EQ;
						case '!':
						default:
							return token = NEQ;
						}
					}
					unread(ch2);
				}
				if (ch == '|' || ch == '&' || ch == '=') {
					int ch2 = read();
					if (ch2 == ch) {
						value = null;
						switch (ch) {
						case '|':
							return token = L_OR;
						case '&':
							return token = L_AND;
						default:
						case '=':
							return token = EQ;
						}
					}
					unread(ch2);
				}
				if (ch == '<' || ch == '>') {
					int ch2 = read();
					if (ch2 == ch) {
						value = null;
						return token = (ch == '<' ? L_SHIFT : R_SHIFT);
					}
					if (ch2 == '=') {
						value = null;
						return token = (ch == '<' ? LE : GE);
					}
					unread(ch2);
				}
				if (isDelim(ch)) {
					if (ch == '.') {
						int ch2 = read();
						if (ch2 >= '0' && ch2 <= '9') {
							buffer.append((char) ch);
							buffer.append((char) ch2);
							state = S_FRAC;
							break;
						}
						unread(ch2);
					}
					value = null;
					return token = ch;
				}
				if (Character.isJavaIdentifierStart((char) ch)) {
					buffer.append((char) ch);
					state = S_ID;
					token = ID;
					break;
				}
				throw new SyntaxException(String.format("syntax error at line %d", getLine()));

			case S_DOUBLE_QUOTE_STRING:
			case S_SINGLE_QUOTE_STRING:
				if (ch == -1) {
					throw new SyntaxException(String.format("unexpected EOF in string at line %d", getLine()));
				}
				if (ch == state) {
					value = buffer.toString();
					return token;
				}
				buffer.append((char) ch);
				break;
			case S_TRIPLE_QUOTE_STRING:
				if (ch == -1) {
					throw new SyntaxException(String.format("unexpected EOF in string at line %d", getLine()));
				}
				if (isTripleQuote(ch)) {
					value = buffer.toString();
					return token;
				}
				buffer.append((char) ch);
				break;

			case S_BINARY:
				if (ch == '0' || ch == '1') {
					buffer.append((char) ch);
				} else {
					unread(ch);
					BigInteger b = BigInteger.valueOf(0);
					BigInteger two = BigInteger.valueOf(2);
					for (int i = 0; i < buffer.length(); i++) {
						if (i != 0) {
							b = b.multiply(two);
						}
						if (buffer.charAt(i) == '1') {
							b = b.add(BigInteger.ONE);
						}
					}
					value = "0x" + b.toString(16);
					return token = NUMBER;
				}
				break;

			case S_HEX:
				if ((ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f') || (ch >= 'A' && ch <= 'F')) {
					buffer.append((char) ch);
				} else {
					unread(ch);
					value = buffer.toString();
					return token = NUMBER;
				}
				break;

			case S_NUMBER:
				if (ch == 'e' || ch == 'E') {
					state = S_EXP;
					buffer.append((char) ch);
				} else if (ch == '.') {
					state = S_FRAC;
					buffer.append((char) ch);
				} else if (ch >= '0' && ch <= '9') {
					buffer.append((char) ch);
				} else {
					unread(ch);
					value = buffer.toString();
					return token = NUMBER;
				}
				break;

			case S_FRAC:
				if (ch == 'e' || ch == 'E') {
					state = S_EXP;
					buffer.append((char) ch);
				} else if (ch >= '0' && ch <= '9') {
					buffer.append((char) ch);
				} else {
					value = buffer.toString();
					unread(ch);
					return token = NUMBER;
				}
				break;

			case S_EXP:
			case S_EXP2:
				if (ch >= '0' && ch <= '9') {
					buffer.append((char) ch);
				} else if (state == S_EXP && (ch == '-' || ch == '+')) {
					buffer.append((char) ch);
					state = S_EXP2;
				} else {
					unread(ch);
					value = buffer.toString();
					return token = NUMBER;
				}
				break;

			case S_ID:
				if (Character.isJavaIdentifierPart((char) ch)) {
					buffer.append((char) ch);
				} else {
					value = buffer.toString();
					unread(ch);
					return token;
				}
				break;

			case S_LINE_COMMENT:
				if (ch == '\n' || ch == EOF) {
					state = S_INIT;
				}
				break;

			case S_BLOCK_COMMENT:
				if (ch == '*') {
					ch = read();
					if (ch == '/') {
						state = S_INIT;
						break;
					}
				}
				if (ch == EOF) {
					throw new SyntaxException(String.format("unexpected EOF in block comment at line %d", getLine()));
				}
				break;

			}
		}
	}

	private boolean isDelim(int ch) {
		return ch == '.' || ch == '{' || ch == '}' || ch == '=' || ch == '+' || ch == '-' || ch == '*' || ch == '/'
				|| ch == '(' || ch == ')' || ch == '[' || ch == ']' || ch == '?' || ch == ':' || ch == ',' || ch == '|'
				|| ch == '^' || ch == '&' || ch == '<' || ch == '>' || ch == '=' || ch == '!' || ch == '~' || ch == '%';
	}

	private boolean isTripleQuote(int ch) throws IOException {
		if (ch != '\"') {
			return false;
		}
		if ((ch = read()) != '\"') {
			unread(ch);
			return false;
		}
		if ((ch = read()) != '\"') {
			unread(ch);
			unread('"');
			return false;
		}
		return true;
	}

	int getLine() {
		return line + 1;
	}

	int getColumn() {
		return col[line % col.length] + 1;
	}

	String getTokenValue() {
		return value;
	}

	int getToken() {
		return token;
	}

	private int read() throws IOException {
		int ch = in.read();
		if (ch == -1)
			return ch;
		if (ch == '\n') {
			++line;
			col[line % col.length] = 1;
		} else {
			++col[line % col.length];
		}
		return ch;
	}

	private void unread(int ch) throws IOException {
		if (ch != -1) {
			if (ch == '\n') {
				--line;
			}
			in.unread(ch);
		}
	}

}
