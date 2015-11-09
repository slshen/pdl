package rpl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;

public class RplScopeTestFixture {
	public RplParser parseFixtures(String... fixtureNames) throws IOException {
		RplParser parser = new RplParser();
		for (String name : fixtureNames) {
			InputStream in = getClass().getResourceAsStream("/" + name);
			Assert.assertNotNull("fixture " + name, in);
			try {
			parser.parse(new InputStreamReader(in), name);
			} finally {
				in.close();
			}
		}
		return parser;
	}

}
