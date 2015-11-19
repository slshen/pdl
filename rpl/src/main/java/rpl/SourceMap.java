package rpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

public class SourceMap {
	private final Map<String, List<String>> sources = new IdentityHashMap<>();

	public Reader injest(Reader reader, String filename) throws IOException {
		final List<String> lines = new ArrayList<>();
		try (BufferedReader in = reader instanceof BufferedReader ? (BufferedReader) reader
				: new BufferedReader(reader)) {
			String line;
			while ((line = in.readLine()) != null) {
				lines.add(line);
			}
		}
		sources.put(filename, lines);
		return new Reader() {
			private int lineNumber, column;
			
			@Override
			public int read(char[] cbuf, int off, int len) throws IOException {
				int n = 0;
				while (n < len) {
					int ch = read();
					if (ch < 0) {
						break;
					}
					cbuf[n++] = (char)ch;
				}
				return n == 0 ? -1 :n;
			}

			@Override
			public void close() throws IOException {
			}

			@Override
			public int read() throws IOException {
				if (lineNumber < lines.size()) {
					String line = lines.get(lineNumber);
					char ch;
					if (column == line.length()) {
						ch = '\n';
						lineNumber++;
						column = 0;
					} else {
						ch = line.charAt(column);
						column++;
					}
					return ch;
				}
				return -1;
			}

		};
	}
	
	public String getLine(String filename, int lineNumber) {
		List<String> lines = sources.get(filename);
		if (lines != null && lineNumber > 0 && lineNumber <= lines.size()) {
			return lines.get(lineNumber - 1);
		}
		return null;
	}

}
