package rpl;

import java.io.IOException;

import org.junit.Test;

public class RplDiagTest extends RplScopeTestFixture {
	
	@Test
	public void testExplain() throws IOException {
		RplParser parser = parseFixtures("ex2.rpl");
		System.out.println(RplDiag.explain(parser, "DB.JDBC_URL"));
	}

}
