package rpl;

import java.io.IOException;

import org.junit.Test;

public class RplDiagTest extends RplScopeTestFixture {
	
	@Test
	public void testExplain() throws IOException {
		RplDiag parser = new RplDiag();
		parseFixtures(parser, "ex2.rpl");
		System.out.println(parser.explain("DB.JDBC_URL"));
		System.out.println(parser.explain("DB_1522.JDBC_URL"));
	}

}
