package pdl;

import java.util.LinkedHashMap;
import java.util.Map;

public class PdlPropertySetNode extends PdlNode {
	private final Map<String, PdlExpressionNode> properties = new LinkedHashMap<>();

	public Map<String, PdlExpressionNode> getProperties() {
		return properties;
	}

}
