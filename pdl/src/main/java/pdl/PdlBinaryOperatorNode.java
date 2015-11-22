package pdl;

public class PdlBinaryOperatorNode extends PdlExpressionNode {
	public static final int L_OR = Tokenizer.L_OR, L_AND = Tokenizer.L_AND,
			LE = Tokenizer.LE, GE = Tokenizer.GE, L_SHIFT = Tokenizer.L_SHIFT,
			R_SHIFT = Tokenizer.R_SHIFT;
	public static final int IN = -100, NOT_IN = -101;
	public static final int EQ = Tokenizer.EQ;
	public static final int NEQ = Tokenizer.NEQ;
	
	private PdlExpressionNode left, right;
	private int operator;

	public PdlExpressionNode getLeft() {
		return left;
	}

	public void setLeft(PdlExpressionNode left) {
		this.left = left;
	}

	public PdlExpressionNode getRight() {
		return right;
	}

	public PdlBinaryOperatorNode withRight(PdlExpressionNode right) {
		this.right = right;
		return this;
	}

	public int getOperator() {
		return operator;
	}

	public void setOperator(int operator) {
		this.operator = operator;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
