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

public class RplParserTest {
	
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
