package pdl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class ExpressionScope extends ValueFunctions {

	private final Map<PdlExpressionNode, Object> values = new HashMap<>();

	abstract Object eval(String name);
	
	abstract List<PdlNode> getTrace();

	Map<PdlExpressionNode, Object> getValues() { return values; }

}
