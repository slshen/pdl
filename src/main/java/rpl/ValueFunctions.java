package rpl;

import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;

public abstract class ValueFunctions {

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
			if (value instanceof Boolean && ((Boolean) value).booleanValue()) {
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

	public Object plus(RplNode node, Object leftValue, Object rightValue) {
		if (leftValue == null) {
			return rightValue;
		}
		Object result;
		BigDecimal leftNumber = asBigDecimal(leftValue);
		BigDecimal rightNumber = asBigDecimal(rightValue);
		if (leftNumber != null && rightNumber != null) {
			result = leftNumber.add(rightNumber);
		} else if (leftValue instanceof Collection<?>) {
			@SuppressWarnings("unchecked")
			Collection<Object> list = (Collection<Object>) leftValue;
			if (rightValue instanceof Collection<?>) {
				list.addAll((Collection<?>) rightValue);
			} else {
				list.add(rightValue);
			}
			result = list;
		} else if (leftValue instanceof Map<?, ?>) {
			@SuppressWarnings("unchecked")
			Map<Object, Object> map = (Map<Object, Object>) leftValue;
			if (rightValue instanceof Map<?, ?>) {
				map.putAll((Map<?, ?>) rightValue);
			} else {
				throw evalException(node, "cannot add a non-dictionary type to a dictionary", null);
			}
			result = map;
		} else {
			result = stringValueOf(leftValue) + stringValueOf(rightValue);
		}
		return result;
	}
	
	protected EvalException evalException(RplNode node, String message, Throwable t) {
		EvalException e = new EvalException(node.diagMessage(message), t);
		e.setSource(node.getSource());
		e.setLine(node.getLine());
		e.setColumn(node.getColumn());
		return e;
	}

}
