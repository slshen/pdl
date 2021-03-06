// Copyright 2019 Sam Shen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package pdl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.junit.Assert;

import pdl.PdlParser;

public class PdlScopeTestFixture {
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
