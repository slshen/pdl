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

import org.junit.Test;

import pdl.PdlDiag;

public class PdlDiagTest extends PdlScopeTestFixture {
	
	@Test
	public void testExplain() throws IOException {
		PdlDiag parser = new PdlDiag();
		parseFixtures(parser, "ex2.pdl");
		System.out.println(parser.explain("DB.JDBC_URL"));
		System.out.println(parser.explain("DB_1522.JDBC_URL"));
	}

}
