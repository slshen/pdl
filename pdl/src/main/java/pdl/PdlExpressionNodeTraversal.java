package pdl;

public class PdlExpressionNodeTraversal {

	private class Traversal extends PdlExpressionNodeVisitor {

		@Override
		public void visit(PdlInvocationNode rplInvocationNode) {
			preVisit(rplInvocationNode);
			if (!rplInvocationNode.isConstructor()) {
				rplInvocationNode.getTarget().accept(this);
			}
			for (PdlExpressionNode node : rplInvocationNode.getArguments()) {
				node.accept(this);
			}
			postVisit(rplInvocationNode);
		}

		@Override
		public void visit(PdlConstantNode rplConstantNode) {
			postVisit(rplConstantNode);
		}

		@Override
		public void visit(PdlBinaryOperatorNode rplBinaryOperatorNode) {
			preVisit(rplBinaryOperatorNode);
			rplBinaryOperatorNode.getLeft().accept(this);
			if (postLeftVisit(rplBinaryOperatorNode)) {
				rplBinaryOperatorNode.getRight().accept(this);
				postVisit(rplBinaryOperatorNode);
			}
		}

		@Override
		public void visit(PdlUnaryOperatorNode rplUnaryOperatorNode) {
			preVisit(rplUnaryOperatorNode);
			rplUnaryOperatorNode.getTarget().accept(this);
			postVisit(rplUnaryOperatorNode);
		}

		@Override
		public void visit(PdlListNode rplListNode) {
			preVisit(rplListNode);
			for (PdlExpressionNode element : rplListNode.getElements()) {
				element.accept(this);
			}
			postVisit(rplListNode);
		}

		@Override
		public void visit(PdlGetValueNode rplGetValueNode) {
			postVisit(rplGetValueNode);
		}

		@Override
		public void visit(PdlAttributeNode rplAttributeNode) {
			preVisit(rplAttributeNode);
			rplAttributeNode.getTarget().accept(this);
			postVisit(rplAttributeNode);
		}

		@Override
		public void visit(PdlSubscriptNode rplSubscriptNode) {
			preVisit(rplSubscriptNode);
			rplSubscriptNode.getTarget().accept(this);
			rplSubscriptNode.getIndex().accept(this);
			postVisit(rplSubscriptNode);
		}

		@Override
		public void visit(PdlDictNode rplDictNode) {
			preVisit(rplDictNode);
			if (rplDictNode.isSet()) {
				for (Object element : rplDictNode.getDict().keySet()) {
					((PdlExpressionNode) element).accept(this);
				}
			} else {
				for (Object value : rplDictNode.getDict().values()) {
					((PdlExpressionNode) value).accept(this);
				}
			}
			postVisit(rplDictNode);
		}

	}

	public void traverse(PdlExpressionNode node) {
		node.accept(new Traversal());
	}

	public void postVisit(PdlDictNode rplDictNode) {
	}

	public void preVisit(PdlDictNode rplDictNode) {
		defaultPreVisit(rplDictNode);
	}

	public void postVisit(PdlSubscriptNode rplSubscriptNode) {
	}

	public void preVisit(PdlSubscriptNode rplSubscriptNode) {
		defaultPreVisit(rplSubscriptNode);
	}

	public boolean postLeftVisit(PdlBinaryOperatorNode rplBinaryOperatorNode) {
		return true;
	}

	public void postVisit(PdlGetValueNode rplGetValueNode) {
	}

	public void postVisit(PdlListNode rplListNode) {
	}

	public void preVisit(PdlListNode rplListNode) {
		defaultPreVisit(rplListNode);
	}

	public void defaultPreVisit(PdlExpressionNode node) {
	}

	public void postVisit(PdlUnaryOperatorNode rplUnaryOperatorNode) {
	}

	public void preVisit(PdlUnaryOperatorNode rplUnaryOperatorNode) {
		defaultPreVisit(rplUnaryOperatorNode);
	}

	public void postVisit(PdlAttributeNode rplAttributeNode) {
	}

	public void preVisit(PdlAttributeNode rplAttributeNode) {
		defaultPreVisit(rplAttributeNode);
	}

	public void postVisit(PdlBinaryOperatorNode rplBinaryOperatorNode) {
	}

	public void preVisit(PdlBinaryOperatorNode rplBinaryOperatorNode) {
		defaultPreVisit(rplBinaryOperatorNode);
	}

	public void postVisit(PdlConstantNode rplConstantNode) {
	}

	public void preVisit(PdlInvocationNode rplInvocationNode) {
		defaultPreVisit(rplInvocationNode);
	}

	public void postVisit(PdlInvocationNode rplInvocationNode) {
	}

}
