// Copyright 2019 Sam Shen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package pdl;

import java.io.IOException;

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
	private final State[] buffer = new State[16];
	private int index = 0, limit = 1;

	public PushbackTokenizer(Tokenizer tokenizer) {
		this.tokenizer = tokenizer;
		buffer[index] = new State(tokenizer);
	}
	
	private int next(int i) { return i == buffer.length - 1 ? 0 : i + 1; }
	private int prev(int i) { return i == 0 ? buffer.length - 1 : i - 1; }
	
	int nextToken() throws IOException {
		if (next(index) == limit) {
			tokenizer.nextToken();
			index = next(index);
			limit = next(limit);
			buffer[index] = new State(tokenizer);
		} else {
			index = next(index);
		}
		return getToken();
	}
	
	void pushback() {
		index = prev(index);
		if (buffer[index] == null || index == limit) {
			throw new IllegalStateException("cannot pusback past start or more than 16 tokens");
		}
	}
	
	int getToken() {
		return buffer[index].token;
	}
	
	String getTokenValue() {
		return buffer[index].tokenValue;
	}
	
	int getLine() {
		return buffer[index].line;
	}

	int getColumn() {
		return buffer[index].column;
	}


}
