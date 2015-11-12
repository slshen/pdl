package rpl;

import java.util.LinkedHashMap;
import java.util.Map;

public class RplDictNode extends RplExpressionNode {

	private final Map<String, RplExpressionNode> dict = new LinkedHashMap<>();

	@Override
	public void accept(RplExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

	public Map<String, RplExpressionNode> getDict() {
		return dict;
	}

}
