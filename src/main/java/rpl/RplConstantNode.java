package rpl;

import java.math.BigDecimal;

public class RplConstantNode extends RplExpressionNode {

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
	public Object accept(RplExpressionNodeVisitor visitor) {
		return visitor.visit(this);
	}

	public boolean isTrue() {
		if (value != null && value.length() > 0) {
			if (value.equalsIgnoreCase("true")) {
				return true;
			}
			BigDecimal n = asNumber();
			return n != null && n.compareTo(BigDecimal.ZERO) != 0;
		}
		return false;
	}

}
