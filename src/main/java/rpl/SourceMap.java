package rpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SourceMap {
	private final Map<String, List<String>> sources = new HashMap<>();

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
				if (lineNumber < lines.size()) {
					String line = lines.get(lineNumber);
					char ch;
					if (column == line.length()) {
						ch = '\n';
					} else {
						ch = line.charAt(column);
					}
					
				}
				return 0;
			}

			@Override
			public void close() throws IOException {
				// TODO Auto-generated method stub
				
			}
			
		};
	}

}
