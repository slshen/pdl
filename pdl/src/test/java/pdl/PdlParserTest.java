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
import java.io.StringReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import pdl.PdlAssignment;
import pdl.PdlConditionalAssignment;
import pdl.PdlConstantNode;
import pdl.PdlParser;

public class PdlParserTest {
	
	@Test
	public void testAssignment() throws IOException {
		PdlParser parser = new PdlParser();
		parser.parse(new StringReader("X = 5"), "input");
		Map<String, PdlAssignment> m = parser.getAssignments();
		Assert.assertTrue(m.containsKey("X"));
		PdlAssignment x = m.get("X");
		Assert.assertEquals(1, x.getConditionalAssignments().size());
		Assert.assertTrue(x.getConditionalAssignments().get(0).getConditions().isEmpty());
		Assert.assertEquals(PdlConstantNode.class, x.getConditionalAssignments().get(0).getValue().getClass());
		Assert.assertEquals("5", ((PdlConstantNode) x.getConditionalAssignments().get(0).getValue()).getValue());
	}
	
	@Test
	public void testCondAssignment() throws IOException {
		PdlParser parser = new PdlParser();
		parser.parse(new StringReader("if (Y) { X = 5 }"), "input");
		Map<String, PdlAssignment> m = parser.getAssignments();
		Assert.assertTrue(m.containsKey("X"));
		PdlAssignment x = m.get("X");
		Assert.assertEquals(1, x.getConditionalAssignments().size());
		PdlConditionalAssignment a = x.getConditionalAssignments().get(0);
		Assert.assertEquals(1, a.getConditions().size());
	}
	
}
