package pdl;

public abstract class PdlExpressionNode extends PdlNode {

	public abstract void accept(PdlExpressionNodeVisitor visitor);

}
