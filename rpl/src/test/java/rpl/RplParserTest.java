package rpl;

import java.io.IOException;
import java.io.StringReader;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

public class RplParserTest {
	
	@Test
	public void testAssignment() throws IOException {
		RplParser parser = new RplParser();
		parser.parse(new StringReader("X = 5"), "input");
		Map<String, RplAssignment> m = parser.getAssignments();
		Assert.assertTrue(m.containsKey("X"));
		RplAssignment x = m.get("X");
		Assert.assertEquals(1, x.getConditionalAssignments().size());
		Assert.assertTrue(x.getConditionalAssignments().get(0).getConditions().isEmpty());
		Assert.assertEquals(RplConstantNode.class, x.getConditionalAssignments().get(0).getValue().getClass());
		Assert.assertEquals("5", ((RplConstantNode) x.getConditionalAssignments().get(0).getValue()).getValue());
	}
	
	@Test
	public void testCondAssignment() throws IOException {
		RplParser parser = new RplParser();
		parser.parse(new StringReader("if (Y) { X = 5 }"), "input");
		Map<String, RplAssignment> m = parser.getAssignments();
		Assert.assertTrue(m.containsKey("X"));
		RplAssignment x = m.get("X");
		Assert.assertEquals(1, x.getConditionalAssignments().size());
		RplConditionalAssignment a = x.getConditionalAssignments().get(0);
		Assert.assertEquals(1, a.getConditions().size());
	}
	
}
