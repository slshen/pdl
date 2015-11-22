package pdl;

public class PdlAttributeNode extends PdlExpressionNode {

	private PdlExpressionNode target;
	private String attributeName;

	public PdlExpressionNode getTarget() {
		return target;
	}

	public void setTarget(PdlExpressionNode base) {
		this.target = base;
	}

	public String getAttributeName() {
		return attributeName;
	}

	public void setAttributeName(String attributeName) {
		this.attributeName = attributeName;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
