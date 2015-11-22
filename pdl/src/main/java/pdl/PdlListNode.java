package pdl;

import java.util.ArrayList;
import java.util.List;

public class PdlListNode extends PdlExpressionNode {
	private final List<PdlExpressionNode> elements = new ArrayList<>();

	public List<PdlExpressionNode> getElements() {
		return elements;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

	
	public boolean isTrue() {
		return !elements.isEmpty();
	}

}
