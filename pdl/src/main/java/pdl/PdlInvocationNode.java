package pdl;

import java.util.ArrayList;
import java.util.List;

public class PdlInvocationNode extends PdlExpressionNode {

	private boolean constructor;
	private String methodName;
	private PdlExpressionNode target;
	private final List<PdlExpressionNode> arguments = new ArrayList<>();

	public boolean isConstructor() {
		return constructor;
	}

	public void setConstructor(boolean constructor) {
		this.constructor = constructor;
	}

	public String getMethodName() {
		return methodName;
	}

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public PdlExpressionNode getTarget() {
		return target;
	}

	public void setTarget(PdlExpressionNode target) {
		this.target = target;
	}

	public List<PdlExpressionNode> getArguments() {
		return arguments;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
