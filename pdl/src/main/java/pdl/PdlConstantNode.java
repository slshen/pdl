package pdl;

import java.math.BigDecimal;

public class PdlConstantNode extends PdlExpressionNode {

	private String value;

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
	public BigDecimal asNumber() {
		try {
			return new BigDecimal(value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}



}
