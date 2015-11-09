package rpl;

public class RplBinaryOperatorNode extends RplExpressionNode {
	public static final int L_OR = Tokenizer.L_OR, L_AND = Tokenizer.L_AND,
			LE = Tokenizer.LE, GE = Tokenizer.GE, L_SHIFT = Tokenizer.L_SHIFT,
			R_SHIFT = Tokenizer.R_SHIFT;
	
	private RplExpressionNode left, right;
	private int operator;

	public RplExpressionNode getLeft() {
		return left;
	}

	public void setLeft(RplExpressionNode left) {
		this.left = left;
	}

	public RplExpressionNode getRight() {
		return right;
	}

	public void setRight(RplExpressionNode right) {
		this.right = right;
	}

	public int getOperator() {
		return operator;
	}

	public void setOperator(int operator) {
		this.operator = operator;
	}

	@Override
	public Object accept(RplExpressionNodeVisitor visitor) {
		return visitor.visit(this);
	}

}
