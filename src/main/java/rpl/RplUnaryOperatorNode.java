package rpl;

public class RplUnaryOperatorNode extends RplExpressionNode {
	
	private int operator;
	private RplExpressionNode target;

	public int getOperator() {
		return operator;
	}

	public void setOperator(int operator) {
		this.operator = operator;
	}

	public RplExpressionNode getTarget() {
		return target;
	}

	public void setTarget(RplExpressionNode target) {
		this.target = target;
	}

	@Override
	public Object accept(RplExpressionNodeVisitor visitor) {
		return visitor.visit(this);
	}

}
