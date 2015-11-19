package rpl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class ExpressionScope extends ValueFunctions {

	private final Map<RplExpressionNode, Object> values = new HashMap<>();

	abstract Object eval(String name);
	
	abstract List<RplNode> getTrace();

	Map<RplExpressionNode, Object> getValues() { return values; }

}
