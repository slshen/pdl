package rpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;

public class RplScopeTestFixture {
	public RplParser parseFixtures(String... fixtureNames) throws IOException {
		RplParser parser = new RplParser();
		parseFixtures(parser, fixtureNames);
		return parser;
	}

	public void parseFixtures(RplParser parser, String... fixtureNames) throws IOException {
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
