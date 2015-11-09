package rpl;

public class RplAttributeNode extends RplExpressionNode {

	private RplExpressionNode base;
	private String attributeName;

	public RplExpressionNode getBase() {
		return base;
	}

	public void setBase(RplExpressionNode base) {
		this.base = base;
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
