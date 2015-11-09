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
		Assert.assertEquals("hello, world", scope.get("X"));
		parser.parse(new StringReader("Y = 'universe'"), "in");
		scope = parser.getResult();
		Assert.assertEquals("universe", scope.get("Y"));
		Assert.assertEquals("hello, universe", scope.get("X"));
	}

}
