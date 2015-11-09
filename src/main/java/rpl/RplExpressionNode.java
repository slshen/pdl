package rpl;

public abstract class RplExpressionNode extends RplNode {

	public abstract void accept(RplExpressionNodeVisitor visitor);

}
