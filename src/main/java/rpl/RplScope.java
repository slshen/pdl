package rpl;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RplScope {
	private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES = new HashMap<>();
	private static final Logger logger = LoggerFactory.getLogger(RplScope.class);

	static {
		PRIMITIVE_TYPES.put(Byte.TYPE, Integer.class);
		PRIMITIVE_TYPES.put(Character.TYPE, Character.class);
		PRIMITIVE_TYPES.put(Short.TYPE, Short.class);
		PRIMITIVE_TYPES.put(Integer.TYPE, Integer.class);
		PRIMITIVE_TYPES.put(Long.TYPE, Long.class);
		PRIMITIVE_TYPES.put(Float.TYPE, Float.class);
		PRIMITIVE_TYPES.put(Double.TYPE, Double.class);
	}

	private static final Object NULL = new Object();
	private final Map<String, RplAssignment> assignments;
	private final ConcurrentMap<String, Object> cache = new ConcurrentHashMap<>();

	private class Evaluator extends RplExpressionNodeTraversal {

		public Object eval(RplExpressionNode expr) {
			traverse(expr);
			return expr.getData();
		}

		@Override
		public void postVisit(RplGetValueNode rplGetValueNode) {
			String name = rplGetValueNode.getName();
			Object value = get(name);
			// since these are mutable values we return copies
			if (value instanceof Map<?, ?>) {
				value = new LinkedHashMap<Object, Object>((Map<?, ?>) value);
			} else if (value instanceof Set<?>) {
				value = new LinkedHashSet<Object>((Set<?>) value);
			} else if (value instanceof Collection<?>) {
				value = new ArrayList<Object>((Collection<?>) value);
			}
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
			Object object = rplAttributeNode.getTarget().getData();
			if (object != null) {
				String attributeName = rplAttributeNode.getAttributeName();
				if (object instanceof Map<?, ?>) {
					rplAttributeNode.setData(((Map<?, ?>) object).get(attributeName));
					return;
				}
				// handle array.length specially
				if (object.getClass().isArray() && attributeName.equals("length")) {
					rplAttributeNode.setData(Array.getLength(object));
					return;
				}
				// look for field
				for (Field field : object.getClass().getFields()) {
					if (field.getName().equals(attributeName)) {
						try {
							rplAttributeNode.setData(field.get(object));
						} catch (IllegalArgumentException | IllegalAccessException e) {
							e.printStackTrace();
						}
						return;
					}
				}
				// look for getter
				String getterName = "get" + Character.toUpperCase(attributeName.charAt(0));
				if (attributeName.length() > 1) {
					getterName += attributeName.substring(1);
				}
				for (Method method : object.getClass().getMethods()) {
					if (method.getName().equals(getterName) && method.getParameterTypes().length == 0) {
						try {
							rplAttributeNode.setData(method.invoke(object));
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							e.printStackTrace();
						}
						return;
					}
				}
			}
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
				result = plus(leftValue, rightValue);
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
			if (rplInvocationNode.isConstructor()) {
				String typeName = rplInvocationNode.getMethodName();
				Class<?> type;
				if (typeName.indexOf('.') < 0) {
					// try java.lang, java.util
					type = findClass("java.lang." + typeName);
					if (type == null) {
						type = findClass("java.util." + typeName);
					}
				} else {
					type = findClass(typeName);
				}
				if (type == null) {
					// XXX warn?
				} else {
					Object[] args = getInvocationArgs(rplInvocationNode);
					for (Constructor<?> ctor : type.getConstructors()) {
						if (isCallableWith(ctor.getParameterTypes(), args)) {
							try {
								Object value = ctor.newInstance(args);
								rplInvocationNode.setData(value);
							} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
									| InvocationTargetException e) {
								logger.warn("call failed", e);
								// XXX ?
							}
							return;
						}
					}
					// XXX warn?
				}
			} else {
				Object object = rplInvocationNode.getTarget().getData();
				if (object == null) {
					// XXX - warn?
					return;
				}
				String name = rplInvocationNode.getMethodName();
				Object[] args = getInvocationArgs(rplInvocationNode);
				for (Method method : object.getClass().getMethods()) {
					if (method.getName().equals(name) && isCallableWith(method.getParameterTypes(), args)) {
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

		private boolean isCallableWith(Class<?>[] parameterTypes, Object[] args) {
			if (parameterTypes.length != args.length) {
				return false;
			}
			for (int i = 0; i < args.length; i++) {
				if (parameterTypes[i].isInstance(args[i])) {
					continue;
				}
				// isInstance returns false for primitive types
				if (parameterTypes[i].isPrimitive()) {
					Class<?> boxedClass = PRIMITIVE_TYPES.get(parameterTypes[i]);
					if (boxedClass != null && boxedClass.isInstance(args[i])) {
						continue;
					}
				}
				return false;
			}
			return true;
		}

		public Object[] getInvocationArgs(RplInvocationNode rplInvocationNode) {
			Object[] args = new Object[rplInvocationNode.getArguments().size()];
			for (int i = 0; i < args.length; i++) {
				args[i] = rplInvocationNode.getArguments().get(i).getData();
			}
			return args;
		}

		private Class<?> findClass(String name) {
			try {
				return Class.forName(name);
			} catch (ClassNotFoundException e) {
				return null;
			}
		}

		@Override
		public void postVisit(RplSubscriptNode rplSubscriptNode) {
			Object object = rplSubscriptNode.getTarget().getData();
			Object index = rplSubscriptNode.getIndex().getData();
			if (object instanceof Collection<?>) {
				BigInteger bi = asBigInteger(index);
				if (bi != null) {
					int i = bi.intValue();
					if (i >= 0 && i < ((Collection<?>) object).size()) {
						if (object instanceof List<?>) {
							rplSubscriptNode.setData(((List<?>) object).get(i));
						} else {
							Iterator<?> iter = ((Collection<?>) object).iterator();
							Object value = iter.next();
							while (i > 0) {
								value = iter.next();
								i = i - 1;
							}
							rplSubscriptNode.setData(value);
						}
					}
					return;
				}
			} else if (object instanceof Map<?, ?> && index != null) {
				Map<?, ?> map = (Map<?, ?>) object;
				rplSubscriptNode.setData(map.get(index));
			} else {
				// XXX - warn? error?
			}
		}

		@Override
		public void postVisit(RplDictNode rplDictNode) {
			if (rplDictNode.isSet()) {
				Set<Object> result = new LinkedHashSet<Object>();
				for (Object element : rplDictNode.getDict().keySet()) {
					result.add(((RplExpressionNode) element).getData());
				}
				rplDictNode.setData(result);
			} else {
				Map<Object, Object> result = new LinkedHashMap<Object, Object>();
				for (Map.Entry<Object, Object> entry : rplDictNode.getDict().entrySet()) {
					result.put(entry.getKey(), ((RplExpressionNode) entry.getValue()).getData());
				}
				rplDictNode.setData(result);
			}
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

	public RplScope(Map<String, RplAssignment> assignments) {
		this.assignments = assignments;
	}

	public Object plus(Object leftValue, Object rightValue) {
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
				// XXX - ?
			}
			result = map;
		} else {
			result = stringValueOf(leftValue) + stringValueOf(rightValue);
		}
		return result;
	}

	public Object get(String name) {
		if (!cache.containsKey(name)) {
			Object value = _get(name);
			cache.putIfAbsent(name, value != null ? value : NULL);
		}
		Object value = cache.get(name);
		return value == NULL ? null : value;
	}

	private Object _get(String name) {
		RplAssignment assignment = assignments.get(name);
		if (assignment == null) {
			return null;
		}
		Object result = null;
		ListIterator<RplConditionalAssignment> iter = assignment.getConditionalAssignments().listIterator();
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
			RplPropertySetNode propertySetNode = conditionalAssignment.getPropertySet();
			if (propertySetNode != null) {
				result = applyPropertySetAssignment(conditionalAssignment, result);
			} else {
				Object value = new Evaluator().eval(conditionalAssignment.getValue());
				if (conditionalAssignment.isAppend()) {
					result = plus(result, value);
				} else {
					result = value;
				}
			}
			if (conditionalAssignment.isOverride()) {
				break;
			}
		}
		return result;
	}

	private Object applyPropertySetAssignment(RplConditionalAssignment conditionalAssignment, Object result) {
		throw new UnsupportedOperationException();
	}

	public Map<String, Object> toMap() {
		Map<String, Object> result = new HashMap<>();
		for (String name : assignments.keySet()) {
			result.put(name, get(name));
		}
		return result;
	}

}
