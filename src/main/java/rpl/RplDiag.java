package rpl;

import java.util.ArrayList;
import java.util.List;

public class RplDiag {
	
	public static String explain(RplParser parser, String name0) {
		String name = name0;
		String propertyName = null;
		int dot = name.indexOf('.');
		if (dot > 0) {
			propertyName = name.substring(dot + 1);
			name = name.substring(0, dot);
		}
		RplScope traceScope = parser.getResult();
		List<RplExpressionNode> trace = new ArrayList<>();
		traceScope.setTrace(trace);
		Object value = traceScope.get(name);
		if (propertyName != null) {
			value = ((RplPropertySet) value).eval(propertyName);
		}
		StringBuilder s = new StringBuilder();
		s.append(name).append(" = ").append(traceScope.stringValueOf(value)).append("\n");
		s.append("comes from:\n");
		String source = null;
		int line = -1;
		for (RplExpressionNode node : trace) {
			if (!node.getSource().equals(source) || line != node.getLine()) {
				source = node.getSource();
				line = node.getLine();
				s.append("    ").append(source).append(" line ").append(line).append("\n");
			}
		}
		return s.toString();
	}

}
