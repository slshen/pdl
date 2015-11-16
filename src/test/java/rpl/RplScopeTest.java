package rpl;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class RplScopeTest extends RplScopeTestFixture {
	
	@Test
	public void testOps() throws IOException {
		RplParser parser = parseFixtures("ops.rpl");
		RplScope scope = parser.getResult();
		Assert.assertEquals("hello, world", scope.get("SAY"));
		parser.parse(new StringReader("SUBJECT = 'universe'"), "in");
		scope = parser.getResult();
		Assert.assertEquals("universe", scope.get("SUBJECT"));
		Assert.assertEquals("hello, universe", scope.get("SAY"));
	}
	
	@Test
	public void testIf() throws IOException {
		RplParser parser = parseFixtures("ops.rpl");
		parser.parse(new StringReader("ENV = 'dev'"), "in");
		RplScope scope = parser.getResult();
		Assert.assertEquals("shouting hello, world", scope.get("SAY"));
	}
	
	@Test
	public void testCalls() throws IOException {
		RplParser parser = parseFixtures("calls.rpl");
		RplScope scope = parser.getResult();
		Assert.assertEquals(Boolean.TRUE, scope.get("IS_PROD"));
		Object path = scope.get("Y");
		Assert.assertTrue(path instanceof String);
		Assert.assertEquals(12, scope.get("Z"));
	}
	
	@Test
	public void testMaps() throws IOException { 
		RplParser parser = parseFixtures("maps.rpl");
		RplScope scope = parser.getResult();
		Assert.assertEquals("hello, world", scope.get("SAY"));
	}
	
	@Test
	public void testToMap() throws IOException {
		RplParser parser = parseFixtures("calls.rpl");
		Map<String, Object> map = parser.getResult().toMap();
		Assert.assertEquals(12, map.get("Z"));
		Assert.assertEquals(Boolean.TRUE, map.get("IS_PROD"));
	}
	
	@Test
	public void testT1() throws IOException {
		RplParser parser = parseFixtures("t1.rpl");
		parser.parse(new StringReader("DB_TYPE = 'oracle'"), "input");
		RplScope scope = parser.getResult();
		Assert.assertEquals("jdbc:oracle:thin:@usdevdb22.example.com:1522", scope.get("LC_DB_URL"));
	}

}
