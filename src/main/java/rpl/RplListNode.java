package rpl;

import java.util.ArrayList;
import java.util.List;

public class RplListNode extends RplExpressionNode {
	private final List<RplExpressionNode> elements = new ArrayList<>();

	public List<RplExpressionNode> getElements() {
		return elements;
	}

	@Override
	public void accept(RplExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

	
	public boolean isTrue() {
		return !elements.isEmpty();
	}

}
