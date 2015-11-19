package rpl;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;

public class SourceMapTest {
	
	@Test
	public void testSourceMapReader() throws IOException {
		SourceMap m = new SourceMap();
		Reader r = m.injest(new StringReader("ab\ncde\n"), "input");
		char[] expected = new char[] { 'a',  'b', '\n', 'c', 'd', 'e', '\n' };
		char[] cbuf = new char[expected.length];
		r.read(cbuf);
		Assert.assertArrayEquals(expected, cbuf);
	}

}
