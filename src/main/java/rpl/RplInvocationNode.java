package rpl;

import java.util.ArrayList;
import java.util.List;

public class RplInvocationNode extends RplExpressionNode {

	private boolean constructor;
	private RplExpressionNode target;
	private final List<RplExpressionNode> arguments = new ArrayList<>();

	public boolean isConstructor() {
		return constructor;
	}

	public void setConstructor(boolean constructor) {
		this.constructor = constructor;
	}

	public RplExpressionNode getTarget() {
		return target;
	}

	public void setTarget(RplExpressionNode target) {
		this.target = target;
	}

	public List<RplExpressionNode> getArguments() {
		return arguments;
	}

	@Override
	public void accept(RplExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
