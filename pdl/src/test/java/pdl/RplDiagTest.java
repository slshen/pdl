package pdl;

import java.io.IOException;

import org.junit.Test;

import pdl.PdlDiag;

public class RplDiagTest extends RplScopeTestFixture {
	
	@Test
	public void testExplain() throws IOException {
		PdlDiag parser = new PdlDiag();
		parseFixtures(parser, "ex2.pdl");
		System.out.println(parser.explain("DB.JDBC_URL"));
		System.out.println(parser.explain("DB_1522.JDBC_URL"));
	}

}
