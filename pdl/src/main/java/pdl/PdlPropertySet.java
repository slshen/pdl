package pdl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdlPropertySet extends ExpressionScope {

	private final Map<String, PdlExpressionNode> expressionNodes = new HashMap<>();
	private final PdlScope scope;

	public PdlPropertySet(PdlScope scope) {
		this.scope = scope;
	}

	public PdlPropertySet(PdlPropertySet value) {
		this.scope = value.scope;
		this.expressionNodes.putAll(value.expressionNodes);
	}

	Map<String, PdlExpressionNode> getExpressionNodes() {
		return expressionNodes;
	}

	@Override
	Object eval(String name) {
		PdlExpressionNode node = expressionNodes.get(name);
		if (node != null) {
			return new Evaluator(this).eval(node);
		}
		return scope.eval(name);
	}

	@Override
	List<PdlNode> getTrace() {
		return scope.getTrace();
	}

}
