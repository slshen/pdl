package rpl;

import java.util.ArrayList;
import java.util.List;

public class RplInvocationNode extends RplExpressionNode {
	
	private boolean constructor;
	private String targetName;
	private final List<RplExpressionNode> arguments = new ArrayList<>();

	public boolean isConstructor() {
		return constructor;
	}

	public void setConstructor(boolean constructor) {
		this.constructor = constructor;
	}

	public String getTargetName() {
		return targetName;
	}

	public void setTargetName(String targetName) {
		this.targetName = targetName;
	}

	public List<RplExpressionNode> getArguments() {
		return arguments;
	}

	@Override
	public Object accept(RplExpressionNodeVisitor visitor) {
		return visitor.visit(this);
	}

}
