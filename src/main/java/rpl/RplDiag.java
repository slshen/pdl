package rpl;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

public class RplDiag extends RplParser {
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
		RplScope traceScope = getResult();
		List<RplNode> trace = new ArrayList<>();
		List<RplNode> propertyTrace = new ArrayList<>();
		traceScope.setTrace(trace);
		Object value = traceScope.eval(name);

		if (propertyName != null) {
			traceScope.setTrace(propertyTrace);
			value = ((RplPropertySet) value).eval(propertyName);
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

	private void printAssignment(StringBuilder s, String name, List<RplNode> trace) {
		int assignments = 0;
		for (int i = trace.size() - 1; i >= 0; --i) {
			RplNode node = trace.get(i);
			if (node instanceof RplConditionalAssignment) {
				RplConditionalAssignment conditionalAssignment = (RplConditionalAssignment) node;
				if (conditionalAssignment.getName().equals(name)) {
					if (assignments > 0) {
						s.append("# and\n");
					}
					print(s, node);
					if (!((RplConditionalAssignment) node).getConditions().isEmpty()) {
						s.append("# (because the following conditions were true)\n");
						for (RplExpressionNode condition : ((RplConditionalAssignment) node).getConditions()) {
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

	private void print(StringBuilder s, RplNode node) {
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

	public static void main(String[] args) {
		try {
			if (args.length == 0) {
				usage("no files");
			}
			RplDiag parser = new RplDiag();
			List<String> explains = new ArrayList<>();
			for (int i = 0; i < args.length; i++) {
				if (args[i].equals("--explain")) {
					if (i + 1 == args.length) {
						usage("not enough arguments for --explain");
					}
					explains.addAll(Arrays.asList(args[i + 1].split("[, ]")));
				} else if (args[i].equals("--help")) {
					usage(null);
				} else {
					parser.parse(new FileReader(args[i]), args[i]);
				}
			}
			if (explains.isEmpty()) {
				Properties properties = new Properties() {
					private static final long serialVersionUID = 1L;

					@Override
					public synchronized Enumeration<Object> keys() {
						Vector<Object> keys = new Vector<>(keySet());
						keys.sort(null);
						return keys.elements();
					}
				};
				parser.getResult().toProperties(properties);
				properties.store(System.out, null);
			} else {
				for (String name : explains) {
					parser.explain(name);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void usage(String msg) {
		if (msg != null)
			System.err.println(msg);
		System.err.println("usage: rpl --explain property file ...");
		System.exit(msg == null ? 0 : 1);
	}

}
