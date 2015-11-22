package pdl;

import java.util.ArrayList;
import java.util.List;

public class PdlConditionalAssignment extends PdlNode {

	private final String name;
	private final List<PdlExpressionNode> conditions = new ArrayList<>();
	private PdlPropertySetNode propertySet;
	private PdlExpressionNode value;
	private boolean override;
	private boolean append;
	
	public PdlConditionalAssignment(String name) {
		this.name =name;
	}
	
	public String getName() {
		return name;
	}

	public List<PdlExpressionNode> getConditions() {
		return conditions;
	}

	public PdlExpressionNode getValue() {
		return value;
	}

	public PdlPropertySetNode getPropertySet() {
		return propertySet;
	}

	public void setPropertySet(PdlPropertySetNode propertySet) {
		this.propertySet = propertySet;
	}

	public void setValue(PdlExpressionNode value) {
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
