package rpl;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

public class PushbackTokenizerTest {
	@Test
	public void testPushback() throws IOException {
		PushbackTokenizer tokenizer = new PushbackTokenizer(
				TokenizerTest.createTokenizer("id \n'foo' = 'bar' , ( z )"));
		for (int i = 0; i < 2; i++) {
			Assert.assertEquals(Tokenizer.ID, tokenizer.nextToken());
			Assert.assertEquals("id", tokenizer.getTokenValue());
			Assert.assertEquals(1, tokenizer.getLine());
			Assert.assertEquals(Tokenizer.STRING, tokenizer.nextToken());
			Assert.assertEquals("foo", tokenizer.getTokenValue());
			Assert.assertEquals(2, tokenizer.getLine());
			tokenizer.pushback();
			tokenizer.pushback();
		}

	}

}
