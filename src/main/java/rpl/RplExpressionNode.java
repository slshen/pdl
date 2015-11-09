package rpl;

public abstract class RplExpressionNode extends RplNode {

	public abstract Object accept(RplExpressionNodeVisitor visitor);

}
