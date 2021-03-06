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
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import pdl.PdlParser;
import pdl.PdlPropertySet;
import pdl.PdlScope;

public class PdlScopeTest extends PdlScopeTestFixture {

	@Test
	public void testHello() throws IOException {
		PdlParser parser = parseFixtures("hello.pdl");
		PdlScope scope = parser.getResult();
		Assert.assertEquals("hello, world", scope.get("SAY"));
		parser.parse(new StringReader("SUBJECT = 'universe'"), "in");
		scope = parser.getResult();
		Assert.assertEquals("universe", scope.get("SUBJECT"));
		Assert.assertEquals("hello, universe", scope.get("SAY"));
	}

	@Test
	public void testIf() throws IOException {
		PdlParser parser = parseFixtures("hello.pdl");
		parser.parse(new StringReader("ENV = 'dev'"), "in");
		PdlScope scope = parser.getResult();
		Assert.assertEquals("shouting hello, world", scope.get("SAY"));
	}

	@Test
	public void testCalls() throws IOException {
		PdlParser parser = parseFixtures("calls.pdl");
		PdlScope scope = parser.getResult();
		Assert.assertEquals(Boolean.TRUE, scope.get("IS_PROD"));
		Object path = scope.get("Y");
		Assert.assertTrue(path instanceof String);
		Assert.assertEquals(12, scope.get("Z"));
	}

	@Test
	public void testMaps() throws IOException {
		PdlParser parser = parseFixtures("maps.pdl");
		PdlScope scope = parser.getResult();
		Assert.assertEquals("hello, world", scope.get("SAY"));
	}

	@Test
	public void testToMap() throws IOException {
		PdlParser parser = parseFixtures("calls.pdl");
		Map<String, Object> map = parser.getResult().toMap();
		Assert.assertEquals(12, map.get("Z"));
		Assert.assertEquals(Boolean.TRUE, map.get("IS_PROD"));
	}

	@Test
	public void testT1() throws IOException {
		PdlParser parser = parseFixtures("t1.pdl");
		parser.parse(new StringReader("DB_TYPE = 'oracle'"), "input");
		PdlScope scope = parser.getResult();
		Assert.assertEquals("jdbc:oracle:thin:@usdevdb22.example.com:1522", scope.get("LC_DB_URL"));
	}

	@Test
	public void testEx2() throws IOException {
		PdlParser parser = parseFixtures("ex2.pdl");
		PdlScope scope = parser.getResult();
		String jdbcUrl = (String) ((PdlPropertySet) scope.get("DB")).eval("JDBC_URL");
		Assert.assertEquals("jdbc:oracle:thin:@oracledev-ex2.example.com:1521/dev", jdbcUrl);
		Map<String, Object> map = scope.toMap();
		//System.out.println(map);
		Assert.assertEquals("jdbc:oracle:thin:@oracledev-ex2.example.com:1521/dev", map.get("DB.JDBC_URL"));
		Assert.assertEquals("jdbc:oracle:thin:@oracledev-ex2.example.com:1522/dev", map.get("DB_1522.JDBC_URL"));
	}

}
