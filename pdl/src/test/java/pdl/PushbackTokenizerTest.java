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

import org.junit.Assert;
import org.junit.Test;

import pdl.PushbackTokenizer;
import pdl.Tokenizer;

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
