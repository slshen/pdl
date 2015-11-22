package pdl;

public class PdlUnaryOperatorNode extends PdlExpressionNode {
	
	private int operator;
	private PdlExpressionNode target;

	public int getOperator() {
		return operator;
	}

	public void setOperator(int operator) {
		this.operator = operator;
	}

	public PdlExpressionNode getTarget() {
		return target;
	}

	public PdlUnaryOperatorNode withTarget(PdlExpressionNode target) {
		this.target = target;
		return this;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
