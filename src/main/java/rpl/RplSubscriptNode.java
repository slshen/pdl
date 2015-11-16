package rpl;

public class RplSubscriptNode extends RplExpressionNode {

	private RplExpressionNode target;
	private RplExpressionNode index;

	public RplExpressionNode getTarget() {
		return target;
	}

	public void setTarget(RplExpressionNode target) {
		this.target = target;
	}

	public RplExpressionNode getIndex() {
		return index;
	}

	public void setIndex(RplExpressionNode index) {
		this.index = index;
	}

	@Override
	public void accept(RplExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
