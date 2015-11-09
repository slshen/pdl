package rpl;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class TokenizerTest {

	@Test
	public void testBinary() throws IOException {
		Tokenizer tokenizer = createTokenizer("0b11110000");
		assertNextTokenValue(tokenizer, Tokenizer.NUMBER, "0xf0");
	}

	@Test
	public void testNumbers() throws IOException {
		assertNextTokenValue(createTokenizer("100"), Tokenizer.NUMBER, "100");
		assertNextTokenValue(createTokenizer("0xff"), Tokenizer.NUMBER, "0xff");
		assertNextTokenValue(createTokenizer("9.875"), Tokenizer.NUMBER, "9.875");
		assertNextTokenValue(createTokenizer("-8.2"), Tokenizer.NUMBER, "-8.2");
		assertNextTokenValue(createTokenizer("+7"), Tokenizer.NUMBER, "+7");
		assertNextTokenValue(createTokenizer("6e5"), Tokenizer.NUMBER, "6e5");
		assertNextTokenValue(createTokenizer("5e-2"), Tokenizer.NUMBER, "5e-2");
		assertNextTokenValue(createTokenizer("4.2e-3"), Tokenizer.NUMBER, "4.2e-3");
	}

	public void testId() throws IOException {
		assertNextTokenValue(createTokenizer("X"), Tokenizer.ID, "X");
	}

	@Test
	public void testLineComment() throws IOException {
		assertTokenValues(createTokenizer("x = 5 # ignore me"), new int[] { Tokenizer.ID, '=', Tokenizer.NUMBER },
				new String[] { "x", null, "5" });
	}

	@Test
	public void testBlockComment() throws IOException {
		assertTokenValues(createTokenizer("x = 5 /* ignore me \n  *\n */\n z = 9"),
				new int[] { Tokenizer.ID, '=', Tokenizer.NUMBER, Tokenizer.ID, '=', Tokenizer.NUMBER },
				new String[] { "x", null, "5", "z", null, "9" });
	}

	private void assertNextTokenValue(Tokenizer tokenizer, int token, String value) throws IOException {
		Assert.assertEquals(token, tokenizer.nextToken());
		Assert.assertEquals(token, tokenizer.getToken());
		Assert.assertEquals(value, tokenizer.getTokenValue());
	}

	private Tokenizer createTokenizer(String s) {
		return new Tokenizer(new StringReader(s));
	}

	private void assertTokenValues(Tokenizer tokenizer, int[] tokens, String[] values) throws IOException {
		int i = 0;
		while (i < tokens.length) {
			int t = tokenizer.nextToken();
			Assert.assertEquals("token # " + i, tokens[i], t);
			if (values[i] != null) {
				Assert.assertEquals("value # " + i, values[i], tokenizer.getTokenValue());
			}
			i++;
		}
		Assert.assertEquals("all tokens consumed", i, tokens.length);
		Assert.assertEquals("no extra tokens", Tokenizer.EOF, tokenizer.nextToken());
	}
}
