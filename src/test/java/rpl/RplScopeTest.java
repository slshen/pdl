package rpl;

import java.io.IOException;
import java.io.StringReader;

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

}
