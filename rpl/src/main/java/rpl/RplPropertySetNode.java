package rpl;

import java.util.LinkedHashMap;
import java.util.Map;

public class RplPropertySetNode extends RplNode {
	private final Map<String, RplExpressionNode> properties = new LinkedHashMap<>();

	public Map<String, RplExpressionNode> getProperties() {
		return properties;
	}

}
