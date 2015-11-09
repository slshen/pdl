package rpl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class PushbackTokenizer {
	private static class State {
		public State(Tokenizer tokenizer) {
			token = tokenizer.getToken();
			tokenValue = tokenizer.getTokenValue();
			line = tokenizer.getLine();
			column = tokenizer.getColumn();
		}
		final int token;
		final String tokenValue;
		final int line;
		final int column;
	}
	private final Tokenizer tokenizer;
	private State top;
	private final List<State> stack = new ArrayList<>();

	public PushbackTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
	
	int nextToken() throws IOException {
		if (!stack.isEmpty()) {
			top = stack.remove(stack.size()-1);
		} else {
			top = new State(tokenizer);
		}
		return getToken();
	}
	
	int getToken() {
		return top != null ? top.token : 0;
	}
	
	String getTokenValue() {
		return top != null ? top.tokenValue : null;
	}
	
	int getLine() {
		return top != null ? top.line : 0;
	}
	
	void pushback() {
		stack.add(top);
	}

	int getColumn() {
		return top != null ? top.column : 0;
	}

}
