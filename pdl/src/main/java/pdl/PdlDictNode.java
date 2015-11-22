package pdl;

import java.util.LinkedHashMap;
import java.util.Map;

public class PdlDictNode extends PdlExpressionNode {

	private final Map<Object, Object> dict = new LinkedHashMap<>();
	private boolean set;

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * For sets, {@link Map#keySet()} contains elements, otherwise this is a map
	 * from {@link String} to {@link PdlExpressionNode}.
	 */
	public Map<Object, Object> getDict() {
		return dict;
	}

	public boolean isSet() {
		return set;
	}

	public void setSet(boolean set) {
		this.set = set;
	}

}
