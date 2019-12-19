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
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Evaluator extends ValueFunctions {
	private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES = new HashMap<>();
	private static final Logger logger = LoggerFactory.getLogger(Evaluator.class);

	static {
		PRIMITIVE_TYPES.put(Byte.TYPE, Integer.class);
		PRIMITIVE_TYPES.put(Character.TYPE, Character.class);
		PRIMITIVE_TYPES.put(Short.TYPE, Short.class);
		PRIMITIVE_TYPES.put(Integer.TYPE, Integer.class);
		PRIMITIVE_TYPES.put(Long.TYPE, Long.class);
		PRIMITIVE_TYPES.put(Float.TYPE, Float.class);
		PRIMITIVE_TYPES.put(Double.TYPE, Double.class);
	}

	private class EvalTraversal extends PdlExpressionNodeTraversal {

		@Override
		public void postVisit(PdlGetValueNode rplGetValueNode) {
			String name = rplGetValueNode.getName();
			Object value = scope.eval(name);
			// since these are mutable values we return copies
			if (value instanceof Map<?, ?>) {
				value = new LinkedHashMap<Object, Object>((Map<?, ?>) value);
			} else if (value instanceof Set<?>) {
				value = new LinkedHashSet<Object>((Set<?>) value);
			} else if (value instanceof Collection<?>) {
				value = new ArrayList<Object>((Collection<?>) value);
			} else if (value instanceof PdlPropertySet) {
				value = new PdlPropertySet((PdlPropertySet) value);
			}
			setValue(rplGetValueNode, value);
		}

		@Override
		public void postVisit(PdlListNode rplListNode) {
			List<Object> values = new ArrayList<>();
			for (PdlExpressionNode element : rplListNode.getElements()) {
				values.add(getValue(element));
			}
			setValue(rplListNode, values);
		}

		@Override
		public void postVisit(PdlUnaryOperatorNode rplUnaryOperatorNode) {
			Object value = getValue(rplUnaryOperatorNode.getTarget());
			switch (rplUnaryOperatorNode.getOperator()) {
			case '+':
			case '-': {
				BigDecimal n = scope.asBigDecimal(value);
				if (n == null)
					n = BigDecimal.ZERO;
				switch (rplUnaryOperatorNode.getOperator()) {
				case '+':
					setValue(rplUnaryOperatorNode, n);
					break;
				case '-':
					setValue(rplUnaryOperatorNode, BigDecimal.ZERO.subtract(n));
					break;
				}
				break;
			}
			case '~': {
				BigInteger n = scope.asBigInteger(value);
				if (n == null)
					setValue(rplUnaryOperatorNode, BigInteger.valueOf(~0));
				else
					setValue(rplUnaryOperatorNode, n.not());
				break;
			}

			case '!':
				if (scope.isTrue(value)) {
					setValue(rplUnaryOperatorNode, "false");
				} else {
					setValue(rplUnaryOperatorNode, "true");
				}
				break;
			default:
				throw evalException(rplUnaryOperatorNode, "unknown operator", null);
			}
		}

		private RuntimeException evalException(PdlExpressionNode expr, String msg, Throwable t) {
			return new PdlEvalException(expr.diagMessage(msg), t);
		}

		@Override
		public void postVisit(PdlAttributeNode rplAttributeNode) {
			Object object = getValue(rplAttributeNode.getTarget());
			if (object != null) {
				String attributeName = rplAttributeNode.getAttributeName();
				if (object instanceof ExpressionScope) {
					ExpressionScope expressionScope = (ExpressionScope) object;
					Object value = expressionScope.eval(attributeName);
					setValue(rplAttributeNode, value);
					return;
				}
				if (object instanceof Map<?, ?>) {
					setValue(rplAttributeNode, ((Map<?, ?>) object).get(attributeName));
					return;
				}
				// handle array.length specially
				if (object.getClass().isArray() && attributeName.equals("length")) {
					setValue(rplAttributeNode, Array.getLength(object));
					return;
				}
				// look for field
				for (Field field : object.getClass().getFields()) {
					if (field.getName().equals(attributeName)) {
						try {
							setValue(rplAttributeNode, field.get(object));
						} catch (IllegalArgumentException | IllegalAccessException e) {
							logger.warn("could not get value of '" + attributeName + "'", e);
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
							setValue(rplAttributeNode, method.invoke(object));
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							logger.warn("could not get value of '" + attributeName + "'", e);
						}
						return;
					}
				}
			}
		}

		@Override
		public boolean postLeftVisit(PdlBinaryOperatorNode rplBinaryOperatorNode) {
			Object leftValue = getValue(rplBinaryOperatorNode.getLeft());
			switch (rplBinaryOperatorNode.getOperator()) {
			case PdlBinaryOperatorNode.L_AND:
				if (!scope.isTrue(leftValue)) {
					setValue(rplBinaryOperatorNode, null);
					return false;
				}
				break;
			case PdlBinaryOperatorNode.L_OR:
				if (scope.isTrue(leftValue)) {
					setValue(rplBinaryOperatorNode, leftValue);
					return false;
				}
				break;
			}
			return true;
		}

		@Override
		public void postVisit(PdlBinaryOperatorNode rplBinaryOperatorNode) {
			Object leftValue = getValue(rplBinaryOperatorNode.getLeft());
			Object rightValue = getValue(rplBinaryOperatorNode.getRight());
			Object result = null;
			switch (rplBinaryOperatorNode.getOperator()) {
			case '+': {
				result = scope.plus(rplBinaryOperatorNode, leftValue, rightValue);
				break;
			}
			case PdlBinaryOperatorNode.EQ:
			case PdlBinaryOperatorNode.NEQ:
			case PdlBinaryOperatorNode.GTE: 
			case PdlBinaryOperatorNode.LTE:
			case '>':
			case '<':
			{
				int op = rplBinaryOperatorNode.getOperator();
				final int cmp;
				if (leftValue == rightValue) {
					cmp = 0;
				} else if (leftValue != null) {
					BigDecimal leftNumber = scope.asBigDecimal(leftValue);
					BigDecimal rightNumber;
					if (leftNumber != null && ((rightNumber = scope.asBigDecimal(rightValue)) != null)) {
						cmp = leftNumber.compareTo(rightNumber);
					} else if (leftValue instanceof Comparable && rightValue instanceof Comparable
							&& leftValue.getClass().isAssignableFrom(rightValue.getClass())) {
						@SuppressWarnings("unchecked")
						Comparable<Object> leftCmp = (Comparable<Object>) leftValue;
						cmp = leftCmp.compareTo(rightValue);
					} else if (op == PdlBinaryOperatorNode.EQ || op == PdlBinaryOperatorNode.NEQ) {
						cmp = leftValue.equals(rightValue) ? 0 : 1;
					} else {
						throw evalException(rplBinaryOperatorNode, "cannot compare non-Comparable objects", null);
					}
				} else {
					cmp = 1;
				}
				switch (op) {
				case PdlBinaryOperatorNode.EQ:
					result = cmp == 0;
					break;
				case PdlBinaryOperatorNode.NEQ:
					result = cmp != 0;
					break;
				case PdlBinaryOperatorNode.GTE:
					result = cmp >= 0;
					break;
				case PdlBinaryOperatorNode.LTE:
					result = cmp <= 0;
					break;
				case '>':
					result = cmp > 0;
					break;
				case '<':
					result = cmp < 0;
					break;
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
				} else if (leftValue instanceof Map<?, ?>) {
					@SuppressWarnings("unchecked")
					Map<Object, Object> map = (Map<Object, Object>) leftValue;
					if (rightValue instanceof Collection<?>) {
						for (Object element : (Collection<?>) rightValue) {
							map.remove(element);
						}
					} else {
						map.remove(rightValue);
					}
				} else {
					BigDecimal leftNumber = scope.asBigDecimal(leftValue);
					BigDecimal rightNumber = scope.asBigDecimal(rightValue);
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
				BigDecimal leftNumber = scope.asBigDecimal(leftValue);
				BigDecimal rightNumber = scope.asBigDecimal(rightValue);
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
			case PdlBinaryOperatorNode.L_SHIFT:
			case PdlBinaryOperatorNode.R_SHIFT: {
				BigInteger leftNumber = scope.asBigInteger(leftValue);
				BigInteger rightNumber = scope.asBigInteger(rightValue);
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
				case PdlBinaryOperatorNode.L_SHIFT:
					result = leftNumber.shiftLeft(rightNumber.intValue());
					break;
				case PdlBinaryOperatorNode.R_SHIFT:
					result = leftNumber.shiftRight(rightNumber.intValue());
					break;
				}
				break;
			}
			case PdlBinaryOperatorNode.L_AND:
			case PdlBinaryOperatorNode.L_OR:
				// in both cases, the shortcut preVisit has done the conditional
				// eval
				result = rightValue;
				break;
			}
			setValue(rplBinaryOperatorNode, result);
		}

		@Override
		public void postVisit(PdlConstantNode rplConstantNode) {
			setValue(rplConstantNode, rplConstantNode.getValue());
		}

		@Override
		public void postVisit(PdlInvocationNode rplInvocationNode) {
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
					throw evalException(rplInvocationNode, "no such class '" + typeName + "'", null);
				} else {
					Object[] args = getInvocationArgs(rplInvocationNode);
					for (Constructor<?> ctor : type.getConstructors()) {
						if (isCallableWith(ctor.getParameterTypes(), args)) {
							try {
								Object value = ctor.newInstance(args);
								setValue(rplInvocationNode, value);
							} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
									| InvocationTargetException e) {
								throw evalException(rplInvocationNode, "constructor for '" + typeName + "' failed", e);
							}
							return;
						}
					}
					throw evalException(rplInvocationNode, "cannot find a constructor for '" + typeName + "'", null);
				}
			} else {
				Object object = getValue(rplInvocationNode.getTarget());
				if (object == null) {
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
							throw evalException(rplInvocationNode, "invocation failed for '" + name + "'", e);
						}
						setValue(rplInvocationNode, value);
						return;
					}
				}
				throw evalException(rplInvocationNode, "cannot find method '" + name + "'", null);
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

		public Object[] getInvocationArgs(PdlInvocationNode rplInvocationNode) {
			Object[] args = new Object[rplInvocationNode.getArguments().size()];
			for (int i = 0; i < args.length; i++) {
				args[i] = getValue(rplInvocationNode.getArguments().get(i));
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
		public void postVisit(PdlSubscriptNode rplSubscriptNode) {
			Object object = getValue(rplSubscriptNode.getTarget());
			Object index = getValue(rplSubscriptNode.getIndex());
			if (object instanceof Collection<?>) {
				BigInteger bi = scope.asBigInteger(index);
				if (bi != null) {
					int i = bi.intValue();
					if (i >= 0 && i < ((Collection<?>) object).size()) {
						if (object instanceof List<?>) {
							setValue(rplSubscriptNode, ((List<?>) object).get(i));
						} else {
							Iterator<?> iter = ((Collection<?>) object).iterator();
							Object value = iter.next();
							while (i > 0) {
								value = iter.next();
								i = i - 1;
							}
							setValue(rplSubscriptNode, value);
						}
					}
					return;
				}
			} else if (object instanceof Map<?, ?> && index != null) {
				Map<?, ?> map = (Map<?, ?>) object;
				setValue(rplSubscriptNode, map.get(index));
			} else {
				logger.warn(rplSubscriptNode.diagMessage("object is not subscriptable"));
			}
		}

		@Override
		public void postVisit(PdlDictNode rplDictNode) {
			if (rplDictNode.isSet()) {
				Set<Object> result = new LinkedHashSet<Object>();
				for (Object element : rplDictNode.getDict().keySet()) {
					result.add(getValue((PdlExpressionNode) element));
				}
				setValue(rplDictNode, result);
			} else {
				Map<Object, Object> result = new LinkedHashMap<Object, Object>();
				for (Map.Entry<Object, Object> entry : rplDictNode.getDict().entrySet()) {
					result.put(entry.getKey(), getValue((PdlExpressionNode) entry.getValue()));
				}
				setValue(rplDictNode, result);
			}
		}
	}

	private final ExpressionScope scope;

	/**
	 * @param rplScope
	 */
	Evaluator(ExpressionScope rplScope) {
		scope = rplScope;
	}

	Object getValue(PdlExpressionNode expression) {
		return scope.getValues().get(expression);
	}

	void setValue(PdlExpressionNode expression, Object value) {
		scope.getValues().put(expression, value);
		List<PdlNode> trace = scope.getTrace();
		if (trace != null) {
			trace.add(expression);
		}
	}

	public Object eval(PdlExpressionNode expr) {
		new EvalTraversal().traverse(expr);
		return getValue(expr);
	}

}