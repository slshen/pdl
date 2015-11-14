package rpl;

import java.util.ArrayList;
import java.util.List;

public class RplConditionalAssignment extends RplNode {
	
	private final List<RplExpressionNode> conditions = new ArrayList<>();
	private RplPropertySetNode propertySet;
	private RplExpressionNode value;
	private boolean override;
	private boolean append;

	public List<RplExpressionNode> getConditions() {
		return conditions;
	}

	public RplExpressionNode getValue() {
		return value;
	}

	public RplPropertySetNode getPropertySet() {
		return propertySet;
	}

	public void setPropertySet(RplPropertySetNode propertySet) {
		this.propertySet = propertySet;
	}

	public void setValue(RplExpressionNode value) {
		this.value = value;
	}

	public boolean isOverride() {
		return override;
	}

	public void setOverride(boolean override) {
		this.override = override;
	}

	public boolean isAppend() {
		return append;
	}

	public void setAppend(boolean append) {
		this.append = append;
	}

}
