package pdl;

import java.io.IOException;

import org.junit.Test;

public class ArithTest extends PdlScopeTestFixture {
	@Test
	public void testArith() throws IOException {
		PdlParser parser = parseFixtures("arith.pdl");
	}
}
