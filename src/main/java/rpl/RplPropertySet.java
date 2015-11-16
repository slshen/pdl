package rpl;

import java.util.HashMap;
import java.util.Map;

public class RplPropertySet extends ExpressionScope {

	private final Map<String, RplExpressionNode> expressionNodes = new HashMap<>();
	private final RplScope scope;

	public RplPropertySet(RplScope scope) {
		this.scope = scope;
	}

	Map<String, RplExpressionNode> getExpressionNodes() {
		return expressionNodes;
	}

	@Override
	Object eval(String name) {
		RplExpressionNode node = expressionNodes.get(name);
		if (node != null) {
			return new Evaluator(this).eval(node);
		}
		return scope.eval(name);
	}

}
