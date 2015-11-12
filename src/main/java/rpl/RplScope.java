package rpl;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
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
			case '-': {
				BigDecimal n = asBigDecimal(value);
				if (n == null)
					n = BigDecimal.ZERO;
				switch (rplUnaryOperatorNode.getOperator()) {
				case '+':
					rplUnaryOperatorNode.setData(n);
					break;
				case '-':
					rplUnaryOperatorNode.setData(BigDecimal.ZERO.subtract(n));
					break;
				}
				break;
			}
			case '~': {
				BigInteger n = asBigInteger(value);
				if (n == null)
					rplUnaryOperatorNode.setData(BigInteger.valueOf(~0));
				else
					rplUnaryOperatorNode.setData(n.not());
			}

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
				} else if (leftValue instanceof Collection<?>) {
					List<Object> list = new ArrayList<>();
					list.addAll((Collection<?>) leftValue);
					if (rightValue instanceof Collection<?>) {
						list.addAll((Collection<?>) rightValue);
					} else {
						list.add(rightValue);
					}
					result = list;
				} else {
					result = stringValueOf(leftValue) + stringValueOf(rightValue);
				}
				break;
			}
			case RplBinaryOperatorNode.EQ: {
				if (leftValue == rightValue) {
					result = true;
				} else if (leftValue != null) {
					BigDecimal leftNumber = asBigDecimal(leftValue);
					BigDecimal rightNumber;
					if (leftNumber != null && ((rightNumber = asBigDecimal(rightValue)) != null)) {
						result = leftNumber.compareTo(rightNumber) == 0;
					} else {
						result = leftValue.equals(rightValue);
					}
				} else {
					result = false;
				}
				break;
			}
			case '-': {
				if (leftValue instanceof Collection<?>) {
					List<Object> list = new ArrayList<>();
					list.addAll((Collection<?>) leftValue);
					if (rightValue instanceof Collection<?>) {
						list.removeAll((Collection<?>) rightValue);
					} else {
						list.remove(rightValue);
					}
					result = list;
				} else {
					BigDecimal leftNumber = asBigDecimal(leftValue);
					BigDecimal rightNumber = asBigDecimal(rightValue);
					if (leftNumber == null)
						leftNumber = BigDecimal.ZERO;
					if (rightNumber == null)
						rightNumber = BigDecimal.ZERO;
					result = leftNumber.subtract(rightNumber);
				}
				break;
			}
			case '*':
			case '/': {
				BigDecimal leftNumber = asBigDecimal(leftValue);
				BigDecimal rightNumber = asBigDecimal(rightValue);
				if (leftNumber == null)
					leftNumber = BigDecimal.ZERO;
				if (rightNumber == null)
					rightNumber = BigDecimal.ZERO;
				switch (rplBinaryOperatorNode.getOperator()) {
				case '*':
					result = leftNumber.multiply(rightNumber);
					break;
				case '/':
					result = leftNumber.divide(rightNumber, MathContext.DECIMAL64);
					break;
				}
				break;
			}
			case '%':
			case '^':
			case '|':
			case '&':
			case RplBinaryOperatorNode.L_SHIFT:
			case RplBinaryOperatorNode.R_SHIFT: {
				BigInteger leftNumber = asBigInteger(leftValue);
				BigInteger rightNumber = asBigInteger(rightValue);
				if (leftNumber == null)
					leftNumber = BigInteger.ZERO;
				if (rightNumber == null)
					rightNumber = BigInteger.ZERO;
				switch (rplBinaryOperatorNode.getOperator()) {
				case '%':
					result = leftNumber.mod(rightNumber);
					break;
				case '^':
					result = leftNumber.xor(rightNumber);
					break;
				case '|':
					result = leftNumber.or(rightNumber);
					break;
				case '&':
					result = leftNumber.and(rightNumber);
					break;
				case RplBinaryOperatorNode.L_SHIFT:
					result = leftNumber.shiftLeft(rightNumber.intValue());
					break;
				case RplBinaryOperatorNode.R_SHIFT:
					result = leftNumber.shiftRight(rightNumber.intValue());
					break;
				}
				break;
			}
			case RplBinaryOperatorNode.L_AND:
			case RplBinaryOperatorNode.L_OR:
				// in both cases, the shortcut preVisit has done the conditional
				// eval
				result = rightValue;
				break;
			}
			rplBinaryOperatorNode.setData(result);
		}

		@Override
		public void postVisit(RplConstantNode rplConstantNode) {
			rplConstantNode.setData(rplConstantNode.getValue());
		}

		@Override
		public void postVisit(RplInvocationNode rplInvocationNode) {
			Object object = rplInvocationNode.getTarget().getData();
			if (object == null) {
				// XXX - warn?
				rplInvocationNode.setData(null);
			} else {
				String name = rplInvocationNode.getMethodName();
				for (Method method : object.getClass().getMethods()) {
					if (method.getName().equals(name)
							&& method.getParameterTypes().length == rplInvocationNode.getArguments().size()) {
						Object[] args = new Object[rplInvocationNode.getArguments().size()];
						for (int i = 0; i < args.length; i++) {
							args[i] = rplInvocationNode.getArguments().get(i).getData();
						}
						Object value;
						try {
							value = method.invoke(object, args);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							// XXX - warn?
							e.printStackTrace();
							value = null;
						}
						rplInvocationNode.setData(value);
						return;
					}
				}
				// XXX - can't find method, warn?
			}
		}

		@Override
		public void postVisit(RplSubscriptNode rplSubscriptNode) {
			throw new UnsupportedOperationException();
		}

	}

	public String stringValueOf(Object value) {
		return value != null ? String.valueOf(value) : "";
	}

	public BigInteger asBigInteger(Object value) {
		if (value instanceof BigInteger) {
			return (BigInteger) value;
		} else if (value instanceof BigDecimal) {
			return ((BigDecimal) value).toBigInteger();
		} else if (value instanceof Number) {
			return BigInteger.valueOf(((Number) value).longValue());
		}
		try {
			return new BigInteger(String.valueOf(value));
		} catch (NumberFormatException e) {
		}
		return null;
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
		try {
			return new BigDecimal(String.valueOf(value));
		} catch (NumberFormatException e) {
		}
		return null;
	}

	public boolean isTrue(Object value) {
		if (value != null) {
			if (value == Boolean.TRUE) {
				return true;
			}
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
		if (assignment == null) {
			return null;
		}
		Object result = null;
		ListIterator<RplConditionalAssignment> iter = assignment.getAssignments().listIterator();
		assignment: while (iter.hasNext()) {
			/*
			 * XXX - should check for override assignments in reverse order so
			 * later sources have precedence
			 */
			RplConditionalAssignment conditionalAssignment = iter.next();
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
