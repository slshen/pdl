package rpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RplPropertySet {
	private final Map<String, RplExpressionNode> properties = new HashMap<>();
	private final List<RplPropertySet> parents = new ArrayList<>();

	public Object getValue(String name) {
		RplExpressionNode node = getExpressionNode(name);
		if (node == null) {
			return null;
		}
		return node.getData();
	}

	public RplExpressionNode getExpressionNode(String name) {
		RplExpressionNode node = properties.get(name);
		if (node == null) {
			for (RplPropertySet parent : parents) {
				node = parent.getExpressionNode(name);
				if (node != null)
					break;
			}
		}
		return node;
	}

	public void put(String name, RplExpressionNode node) {
		properties.put(name, node);
	}

	public void addParent(RplPropertySet parent) {
		// XXX - should check for duplicate properties?
		parents.add(parent);
	}

}
