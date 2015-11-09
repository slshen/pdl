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

	public RplUnaryOperatorNode withTarget(RplExpressionNode target) {
		this.target = target;
		return this;
	}

	@Override
	public void accept(RplExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
