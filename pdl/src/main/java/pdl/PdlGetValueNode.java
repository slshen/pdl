package pdl;

public class PdlGetValueNode extends PdlExpressionNode {
	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
