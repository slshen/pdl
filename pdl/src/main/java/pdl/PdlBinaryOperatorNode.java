// Copyright 2019 Sam Shen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package pdl;

public class PdlBinaryOperatorNode extends PdlExpressionNode {
	public static final int L_OR = Tokenizer.L_OR, L_AND = Tokenizer.L_AND,
			LE = Tokenizer.LTE, GE = Tokenizer.GTE, L_SHIFT = Tokenizer.L_SHIFT,
			R_SHIFT = Tokenizer.R_SHIFT;
	public static final int IN = -100, NOT_IN = -101;
	public static final int EQ = Tokenizer.EQ;
	public static final int NEQ = Tokenizer.NEQ;
	public static final int GTE = Tokenizer.GTE;
	public static final int LTE = Tokenizer.LTE;
	
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
