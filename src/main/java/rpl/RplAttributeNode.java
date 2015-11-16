package rpl;

public class RplAttributeNode extends RplExpressionNode {

	private RplExpressionNode target;
	private String attributeName;

	public RplExpressionNode getTarget() {
		return target;
	}

	public void setTarget(RplExpressionNode base) {
		this.target = base;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public void accept(RplExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
