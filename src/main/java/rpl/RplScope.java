package rpl;

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

public class RplScope extends ExpressionScope {

	private static final Object NULL = new Object();
	private final Map<String, RplAssignment> assignments;
	private final Map<String, Object> cache = new HashMap<>();
	private List<RplExpressionNode> trace;

	RplScope(Map<String, RplAssignment> assignments) {
		this.assignments = assignments;
	}
	
	@Override
	List<RplExpressionNode> getTrace() {
		return trace;
	}
	
	void setTrace(List<RplExpressionNode> trace) {
		this.trace = trace;
	}

	public Object get(String name) {
		return eval(name);
	}

	@Override
	Object eval(String name) {
		if (!cache.containsKey(name)) {
			// XXX - put marker in cache to detect cyclic dependencies?
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
				if (!isTrue(new Evaluator(this).eval(cond))) {
					continue assignment;
				}
			}
			RplPropertySetNode propertySetNode = conditionalAssignment.getPropertySet();
			if (propertySetNode != null) {
				result = applyPropertySetNodeAssignment(conditionalAssignment, result);
			} else {
				Object value = new Evaluator(this).eval(conditionalAssignment.getValue());
				if (value instanceof RplPropertySet) {
					result = applyPropertySetAssignment(conditionalAssignment, result, value);
				} else {
					if (conditionalAssignment.isAppend()) {
						result = plus(conditionalAssignment, result, value);
					} else {
						result = value;
					}
				}
			}
			if (conditionalAssignment.isOverride()) {
				break;
			}
		}
		return result;
	}

	private Object applyPropertySetAssignment(RplConditionalAssignment conditionalAssignment, Object result,
			Object value) {
		if (conditionalAssignment.isAppend()) {
			if (result != null && !(result instanceof RplPropertySet)) {
				throw evalException(conditionalAssignment, "cannot append property sets to non-property sets", null);
			}
		}
		RplPropertySet rightPropertySet = (RplPropertySet) value;
		RplPropertySet propertySet;
		if (result instanceof RplPropertySet) {
			propertySet = (RplPropertySet) result;
		} else {
			propertySet = new RplPropertySet(this);
		}
		if (!conditionalAssignment.isAppend()) {
			propertySet.getExpressionNodes().clear();
		}
		propertySet.getExpressionNodes().putAll(rightPropertySet.getExpressionNodes());
		return propertySet;
	}

	private Object applyPropertySetNodeAssignment(RplConditionalAssignment conditionalAssignment, Object result) {
		RplPropertySet propertySet;
		if (result == null) {
			propertySet = new RplPropertySet(this);
		} else if (result instanceof RplPropertySet) {
			propertySet = (RplPropertySet) result;
		} else {
			throw evalException(conditionalAssignment, "can only add properties to a property set", null);
		}
		propertySet.getExpressionNodes().putAll(conditionalAssignment.getPropertySet().getProperties());
		return propertySet;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> result = new HashMap<>();
		for (String name : assignments.keySet()) {
			Object value = get(name);
			if (value instanceof RplPropertySet) {
				RplPropertySet propertySet = (RplPropertySet) value;
				for (String propertyName : propertySet.getExpressionNodes().keySet()) {
					Object propertyValue = propertySet.eval(propertyName);
					result.put(name + "." + propertyName, propertyValue);
				}
			} else {
				result.put(name, value);
			}
		}
		return result;
	}

}
