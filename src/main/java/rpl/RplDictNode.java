package rpl;

import java.util.LinkedHashMap;
import java.util.Map;

public class RplDictNode extends RplExpressionNode {

	private final Map<Object, Object> dict = new LinkedHashMap<>();
	private boolean set;

	@Override
	public void accept(RplExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * For sets, {@link Map#keySet()} contains elements, otherwise this is a map
	 * from {@link String} to {@link RplExpressionNode}.
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
