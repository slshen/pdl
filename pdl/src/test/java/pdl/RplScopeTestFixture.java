package pdl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;

import pdl.PdlParser;

public class RplScopeTestFixture {
	public PdlParser parseFixtures(String... fixtureNames) throws IOException {
		PdlParser parser = new PdlParser();
		parseFixtures(parser, fixtureNames);
		return parser;
	}

	public void parseFixtures(PdlParser parser, String... fixtureNames) throws IOException {
		for (String name : fixtureNames) {
			InputStream in = getClass().getResourceAsStream("/" + name);
			Assert.assertNotNull("fixture " + name, in);
			try {
				parser.parse(new InputStreamReader(in), name);
			} finally {
				in.close();
			}
		}
	}

}
