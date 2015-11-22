package pdl;

public class PdlSubscriptNode extends PdlExpressionNode {

	private PdlExpressionNode target;
	private PdlExpressionNode index;

	public PdlExpressionNode getTarget() {
		return target;
	}

	public void setTarget(PdlExpressionNode target) {
		this.target = target;
	}

	public PdlExpressionNode getIndex() {
		return index;
	}

	public void setIndex(PdlExpressionNode index) {
		this.index = index;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
