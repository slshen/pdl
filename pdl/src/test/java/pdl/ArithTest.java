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
import java.io.StringReader;

import org.junit.Assert;
import org.junit.Test;


public class ArithTest extends PdlScopeTestFixture {
	@Test
	public void testArith() throws IOException {
		PdlParser parser = parseFixtures("arith.pdl");
		PdlScope scope = parser.getResult();
		assertEquals(17, scope.get("PLUS"));
		assertEquals(3, scope.get("MINUS"));
		assertEquals(-10, scope.get("NEG"));
		assertEquals(7, scope.get("POS"));
		assertEquals(3, scope.get("MOD"));
		assertEquals(10 | 7, scope.get("OR"));
		assertEquals(10 & 7, scope.get("AND"));
		assertEquals(10 ^ 7, scope.get("XOR"));
		assertEquals(10 >> 1, scope.get("SR"));
		assertEquals(7 << 2, scope.get("SL"));
		Assert.assertEquals(Boolean.FALSE, scope.get("EQ"));
		Assert.assertEquals(Boolean.TRUE, scope.get("NEQ"));
	}
	
	@Test
	public void testFpArith() throws IOException {
		PdlParser parser = parseFixtures("arith.pdl");
		parser.parse(new StringReader("A = 15.0 B = 9.5"), "in");
		PdlScope scope = parser.getResult();
		assertEquals(24.5, scope.get("PLUS"));
		assertEquals(5.5, scope.get("MINUS"));
		assertEquals(-15.0, scope.get("NEG"));
		assertEquals(9.5, scope.get("POS"));
		Assert.assertEquals(Boolean.FALSE, scope.get("EQ"));
		Assert.assertEquals(Boolean.TRUE, scope.get("NEQ"));
	}
	
	private static void assertEquals(double a, Object b) {
		Assert.assertEquals(String.valueOf(a), String.valueOf(b));
	}
	
	private static void assertEquals(int a, Object b) {
		Assert.assertEquals(a, ((Number) b).intValue());
	}
}
