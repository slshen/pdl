package rpl;

public class RplExpressionNodeTraversal {

	private class Traversal extends RplExpressionNodeVisitor {

		@Override
		public void visit(RplInvocationNode rplInvocationNode) {
			preVisit(rplInvocationNode);
			if (!rplInvocationNode.isConstructor()) {
				rplInvocationNode.getTarget().accept(this);
			}
			for (RplExpressionNode node : rplInvocationNode.getArguments()) {
				node.accept(this);
			}
			postVisit(rplInvocationNode);
		}

		@Override
		public void visit(RplConstantNode rplConstantNode) {
			postVisit(rplConstantNode);
		}

		@Override
		public void visit(RplBinaryOperatorNode rplBinaryOperatorNode) {
			preVisit(rplBinaryOperatorNode);
			rplBinaryOperatorNode.getLeft().accept(this);
			if (postLeftVisit(rplBinaryOperatorNode)) {
				rplBinaryOperatorNode.getRight().accept(this);
				postVisit(rplBinaryOperatorNode);
			}
		}

		@Override
		public void visit(RplUnaryOperatorNode rplUnaryOperatorNode) {
			preVisit(rplUnaryOperatorNode);
			rplUnaryOperatorNode.getTarget().accept(this);
			postVisit(rplUnaryOperatorNode);
		}

		@Override
		public void visit(RplListNode rplListNode) {
			preVisit(rplListNode);
			for (RplExpressionNode element : rplListNode.getElements()) {
				element.accept(this);
			}
			postVisit(rplListNode);
		}

		@Override
		public void visit(RplGetValueNode rplGetValueNode) {
			postVisit(rplGetValueNode);
		}

		@Override
		public void visit(RplAttributeNode rplAttributeNode) {
			preVisit(rplAttributeNode);
			rplAttributeNode.getTarget().accept(this);
			postVisit(rplAttributeNode);
		}

		@Override
		public void visit(RplSubscriptNode rplSubscriptNode) {
			preVisit(rplSubscriptNode);
			rplSubscriptNode.getTarget().accept(this);
			rplSubscriptNode.getIndex().accept(this);
			postVisit(rplSubscriptNode);
		}

		@Override
		public void visit(RplDictNode rplDictNode) {
			preVisit(rplDictNode);
			for (RplExpressionNode value : rplDictNode.getDict().values()) {
				value.accept(this);
			}
			postVisit(rplDictNode);
		}

	}

	public void traverse(RplExpressionNode node) {
		node.accept(new Traversal());
	}

	public void postVisit(RplDictNode rplDictNode) {
	}

	public void preVisit(RplDictNode rplDictNode) {
		defaultPreVisit(rplDictNode);
	}

	public void postVisit(RplSubscriptNode rplSubscriptNode) {
	}

	public void preVisit(RplSubscriptNode rplSubscriptNode) {
		defaultPreVisit(rplSubscriptNode);
	}

	public boolean postLeftVisit(RplBinaryOperatorNode rplBinaryOperatorNode) {
		return true;
	}

	public void postVisit(RplGetValueNode rplGetValueNode) {
	}

	public void postVisit(RplListNode rplListNode) {
	}

	public void preVisit(RplListNode rplListNode) {
		defaultPreVisit(rplListNode);
	}

	public void defaultPreVisit(RplExpressionNode node) {
	}

	public void postVisit(RplUnaryOperatorNode rplUnaryOperatorNode) {
	}

	public void preVisit(RplUnaryOperatorNode rplUnaryOperatorNode) {
		defaultPreVisit(rplUnaryOperatorNode);
	}

	public void postVisit(RplAttributeNode rplAttributeNode) {
	}

	public void preVisit(RplAttributeNode rplAttributeNode) {
		defaultPreVisit(rplAttributeNode);
	}

	public void postVisit(RplBinaryOperatorNode rplBinaryOperatorNode) {
	}

	public void preVisit(RplBinaryOperatorNode rplBinaryOperatorNode) {
		defaultPreVisit(rplBinaryOperatorNode);
	}

	public void postVisit(RplConstantNode rplConstantNode) {
	}

	public void preVisit(RplInvocationNode rplInvocationNode) {
		defaultPreVisit(rplInvocationNode);
	}

	public void postVisit(RplInvocationNode rplInvocationNode) {
	}

}
