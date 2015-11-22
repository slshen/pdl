package pdl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class PdlDiag extends PdlParser {
	private final SourceMap sourceMap = new SourceMap();

	public String explain(String fullname) {
		String name = fullname;
		String propertyName = null;
		int dot = name.indexOf('.');
		if (dot > 0) {
			propertyName = name.substring(dot + 1);
			name = name.substring(0, dot);
		}
		// get base
		PdlScope traceScope = getResult();
		List<PdlNode> trace = new ArrayList<>();
		List<PdlNode> propertyTrace = new ArrayList<>();
		traceScope.setTrace(trace);
		Object value = traceScope.eval(name);

		if (propertyName != null) {
			traceScope.setTrace(propertyTrace);
			value = ((PdlPropertySet) value).eval(propertyName);
		}

		StringBuilder s = new StringBuilder();
		s.append("# ").append(fullname).append(" = ").append(value);
		if (propertyName != null) {
			s.append("\n# comes from the property ").append(propertyName).append(" on ").append(name).append("\n");
			print(s, propertyTrace.get(propertyTrace.size() - 1));
			s.append("# the property set ").append(name).append(" comes from\n");
			printAssignment(s, name, trace);
		} else {
			s.append("\n# comes from ").append(name).append("\n");
			printAssignment(s, name, trace);
		}
		return s.toString();
	}

	private void printAssignment(StringBuilder s, String name, List<PdlNode> trace) {
		int assignments = 0;
		for (int i = trace.size() - 1; i >= 0; --i) {
			PdlNode node = trace.get(i);
			if (node instanceof PdlConditionalAssignment) {
				PdlConditionalAssignment conditionalAssignment = (PdlConditionalAssignment) node;
				if (conditionalAssignment.getName().equals(name)) {
					if (assignments > 0) {
						s.append("# and\n");
					}
					print(s, node);
					if (!((PdlConditionalAssignment) node).getConditions().isEmpty()) {
						s.append("# (because the following conditions were true)\n");
						for (PdlExpressionNode condition : ((PdlConditionalAssignment) node).getConditions()) {
							print(s, condition);
						}
					}
					assignments++;
					if (!conditionalAssignment.isAppend()) {
						break;
					}
				}
			}
		}
	}

	private void print(StringBuilder s, PdlNode node) {
		s.append(node.getSource()).append(":").append(node.getLine());
		// s.append(" (").append(node.getClass().getSimpleName()).append(")");

		String line = sourceMap.getLine(node.getSource(), node.getLine());
		if (line != null) {
			s.append(" ").append(line);
		} else {
			s.append(" <unknown source>");
		}
		s.append("\n");
	}

	@Override
	public void parse(Reader in, String filename) throws IOException {
		filename = new String(filename);
		Reader reader = sourceMap.injest(in, filename);
		super.parse(reader, filename);
	}

}
