package rpl;

import java.util.HashMap;
import java.util.Map;

abstract class ExpressionScope extends ValueFunctions {

	private final Map<RplExpressionNode, Object> values = new HashMap<>();

	abstract Object eval(String name);

	Map<RplExpressionNode, Object> getValues() { return values; }

}
