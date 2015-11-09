package rpl;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class RplScope {

	private final Map<String, RplAssignment> assignments;

	private class Evaluator extends RplExpressionNodeTraversal {

		public Object eval(RplExpressionNode expr) {
			traverse(expr);
			return expr.getData();
		}

		@Override
		public void postVisit(RplGetValueNode rplGetValueNode) {
			String name = rplGetValueNode.getName();
			Object value = get(name);
			rplGetValueNode.setData(value);
		}

		@Override
		public void postVisit(RplListNode rplListNode) {
			List<Object> values = new ArrayList<>();
			for (RplExpressionNode element : rplListNode.getElements()) {
				values.add(element.getData());
			}
			rplListNode.setData(values);
		}

		@Override
		public void defaultPreVisit(RplExpressionNode node) {
			node.setData(null);
		}

		@Override
		public void postVisit(RplUnaryOperatorNode rplUnaryOperatorNode) {
			Object value = rplUnaryOperatorNode.getTarget().getData();
			switch (rplUnaryOperatorNode.getOperator()) {
			case '+':
			case '-':
			case '~':
				BigDecimal n = asBigDecimal(value);
				if (n == null) {
					throw evalException(rplUnaryOperatorNode, "argument is not a number");
				}
				switch (rplUnaryOperatorNode.getOperator()) {
				case '+':
					rplUnaryOperatorNode.setData(n);
					break;
				case '-':
					rplUnaryOperatorNode.setData(BigDecimal.ZERO.subtract(n));
					break;
				case '~':
					byte[] bytes = n.toBigIntegerExact().toByteArray();
					for (int i = 0; i < bytes.length; i++) {
						bytes[i] = (byte) ~bytes[i];
					}
					rplUnaryOperatorNode.setData(new BigInteger(bytes));
					break;
				}
				break;
			case '!':
				if (isTrue(value)) {
					rplUnaryOperatorNode.setData("false");
				} else {
					rplUnaryOperatorNode.setData("true");
				}
				break;
			default:
				throw evalException(rplUnaryOperatorNode, "unknown operator");
			}
		}

		private RuntimeException evalException(RplExpressionNode expr, String msg) {
			return new RplEvalException(String.format("%s:%d %s", expr.getSource(), expr.getLine(), msg));
		}

		@Override
		public void postVisit(RplAttributeNode rplAttributeNode) {
			super.postVisit(rplAttributeNode);
		}

		@Override
		public boolean postLeftVisit(RplBinaryOperatorNode rplBinaryOperatorNode) {
			Object leftValue = rplBinaryOperatorNode.getLeft().getData();
			switch (rplBinaryOperatorNode.getOperator()) {
			case RplBinaryOperatorNode.L_AND:
				if (!isTrue(leftValue)) {
					rplBinaryOperatorNode.setData(null);
					return false;
				}
				break;
			case RplBinaryOperatorNode.L_OR:
				if (isTrue(leftValue)) {
					rplBinaryOperatorNode.setData(leftValue);
					return false;
				}
				break;
			}
			return true;
		}

		@Override
		public void postVisit(RplBinaryOperatorNode rplBinaryOperatorNode) {
			Object leftValue = rplBinaryOperatorNode.getLeft().getData();
			Object rightValue = rplBinaryOperatorNode.getRight().getData();
			Object result = null;
			switch (rplBinaryOperatorNode.getOperator()) {
			case '+': {
				BigDecimal leftNumber = asBigDecimal(leftValue);
				BigDecimal rightNumber = asBigDecimal(rightValue);
				if (leftNumber != null && rightNumber != null) {
					result = leftNumber.add(rightNumber);
				} else if (leftValue instanceof Collection<?> && rightValue instanceof Collection<?>) {
					List<Object> list = new ArrayList<>();
					list.addAll((Collection<?>) leftValue);
					list.addAll((Collection<?>) rightValue);
					result = list;
				} else {
					result = stringValueOf(leftValue) + stringValueOf(rightValue);
				}
				break;
			}
			case '-':
			case '*':
			case '/':
			case '%':
			case '^':
			case '|':
			case '&':
			case RplBinaryOperatorNode.L_SHIFT:
			case RplBinaryOperatorNode.R_SHIFT:
			case RplBinaryOperatorNode.L_AND:
			case RplBinaryOperatorNode.L_OR:
				throw new UnsupportedOperationException();
			}
			rplBinaryOperatorNode.setData(result);

		}

		@Override
		public void postVisit(RplConstantNode rplConstantNode) {
			rplConstantNode.setData(rplConstantNode.getValue());
		}

		@Override
		public void postVisit(RplInvocationNode rplInvocationNode) {
			// TODO Auto-generated method stub
			super.postVisit(rplInvocationNode);
		}
	}

	public String stringValueOf(Object value) {
		return value != null ? String.valueOf(value) : "";
	}

	public BigDecimal asBigDecimal(Object value) {
		if (value instanceof BigDecimal) {
			return (BigDecimal) value;
		}
		if (value instanceof Number) {
			if (value instanceof Float || value instanceof Double) {
				return BigDecimal.valueOf(((Number) value).doubleValue());
			}
			return BigDecimal.valueOf(((Number) value).longValue());
		}
		if (value instanceof String) {
			try {
				return new BigDecimal((String) value);
			} catch (NumberFormatException e) {
			}
		}
		return null;
	}

	public boolean isTrue(Object value) {
		if (value != null) {
			if (value instanceof String) {
				String s = (String) value;
				if (s.equalsIgnoreCase("true")) {
					return true;
				}
				try {
					BigDecimal n = new BigDecimal(s);
					return n.compareTo(BigDecimal.ZERO) != 0;
				} catch (NumberFormatException e) {
				}
			} else if (value instanceof Collection<?>) {
				return !((Collection<?>) value).isEmpty();
			} else if (value.getClass().isArray()) {
				return Array.getLength(value) > 0;
			} else if (value instanceof Map<?, ?>) {
				return !((Map<?, ?>) value).isEmpty();
			}
		}
		return false;
	}

	public RplScope(Map<String, RplAssignment> assignments) {
		this.assignments = assignments;
	}

	public Object get(String name) {
		RplAssignment assignment = assignments.get(name);
		Object result = null;
		ListIterator<RplConditionalAssignment> iter = assignment.getAssignments()
				.listIterator(assignment.getAssignments().size());
		assignment: while (iter.hasPrevious()) {
			// NB - evaluate in reverse order so that later sources have
			// precedence
			RplConditionalAssignment conditionalAssignment = iter.previous();
			for (RplExpressionNode cond : conditionalAssignment.getConditions()) {
				if (!isTrue(new Evaluator().eval(cond))) {
					continue assignment;
				}
			}
			Object value = new Evaluator().eval(conditionalAssignment.getValue());
			if (conditionalAssignment.isAppend()) {
				if (result == null) {
					result = new ArrayList<Object>();
				}
				if (result instanceof List<?>) {
					@SuppressWarnings("unchecked")
					List<Object> list = (List<Object>) result;
					list.add(value);
				} else {
					result = result.toString() + value;
				}
			} else if (conditionalAssignment.isOverride()) {
				return value;
			} else {
				result = value;
			}
		}
		return result;
	}

}
