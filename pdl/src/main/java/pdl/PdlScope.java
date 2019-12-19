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

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;

/**
 * PdlScope represents a set of names and values.
 */
public class PdlScope extends ExpressionScope {

	private static final Object NULL = new Object();
	private final Map<String, PdlAssignment> assignments;
	private final Map<String, Object> cache = new HashMap<>();
	private List<PdlNode> trace;

	PdlScope(Map<String, PdlAssignment> assignments) {
		this.assignments = assignments;
	}
	
	@Override
	List<PdlNode> getTrace() {
		return trace;
	}
	
	void setTrace(List<PdlNode> trace) {
		this.trace = trace;
	}

	/**
	 * Returns the value of <code>name</code> in the current scope.
	 */
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
		PdlAssignment assignment = assignments.get(name);
		if (assignment == null) {
			return null;
		}
		Object result = null;
		ListIterator<PdlConditionalAssignment> iter = assignment.getConditionalAssignments().listIterator();
		assignment: while (iter.hasNext()) {
			/*
			 * XXX - should check for override assignments in reverse order so
			 * later sources have precedence
			 */
			PdlConditionalAssignment conditionalAssignment = iter.next();
			for (PdlExpressionNode cond : conditionalAssignment.getConditions()) {
				if (!isTrue(new Evaluator(this).eval(cond))) {
					continue assignment;
				}
			}
			if (trace != null) {
				trace.add(conditionalAssignment);
			}
			PdlPropertySetNode propertySetNode = conditionalAssignment.getPropertySet();
			if (propertySetNode != null) {
				result = applyPropertySetNodeAssignment(conditionalAssignment, result);
			} else {
				Object value = new Evaluator(this).eval(conditionalAssignment.getValue());
				if (value instanceof PdlPropertySet) {
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

	private Object applyPropertySetAssignment(PdlConditionalAssignment conditionalAssignment, Object result,
			Object value) {
		if (conditionalAssignment.isAppend()) {
			if (result != null && !(result instanceof PdlPropertySet)) {
				throw evalException(conditionalAssignment, "cannot append property sets to non-property sets", null);
			}
		}
		PdlPropertySet rightPropertySet = (PdlPropertySet) value;
		PdlPropertySet propertySet;
		if (result instanceof PdlPropertySet) {
			propertySet = (PdlPropertySet) result;
		} else {
			propertySet = new PdlPropertySet(this);
		}
		if (!conditionalAssignment.isAppend()) {
			propertySet.getExpressionNodes().clear();
		}
		propertySet.getExpressionNodes().putAll(rightPropertySet.getExpressionNodes());
		return propertySet;
	}

	private Object applyPropertySetNodeAssignment(PdlConditionalAssignment conditionalAssignment, Object result) {
		PdlPropertySet propertySet;
		if (result == null) {
			propertySet = new PdlPropertySet(this);
		} else if (result instanceof PdlPropertySet) {
			propertySet = (PdlPropertySet) result;
		} else {
			throw evalException(conditionalAssignment, "can only add properties to a property set", null);
		}
		propertySet.getExpressionNodes().putAll(conditionalAssignment.getPropertySet().getProperties());
		return propertySet;
	}

	/**
	 * Converts the current scope to a <code>Map&lt;String,Object&gt;</code>.
	 * Nested property values are flattened with "." as a separator
	 * in their names.
	 */
	public Map<String, Object> toMap() {
		Map<String, Object> result = new HashMap<>();
		for (String name : assignments.keySet()) {
			Object value = get(name);
			if (value instanceof PdlPropertySet) {
				PdlPropertySet propertySet = (PdlPropertySet) value;
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

	/**
	 * Copies the current scope to a <code>Properties</code> object.
	 * Keys are converted as in {@link #toMap()}.  Values are converted
	 * to strings (null values are converted to empty strings.)
	 */
	public Properties toProperties(Properties properties) {
		Map<String, Object> map = toMap();
		for (Map.Entry<String, Object> entry : map.entrySet()) {
			properties.setProperty(entry.getKey(), stringValueOf(entry.getValue()));
		}
		return properties;
	}

}
