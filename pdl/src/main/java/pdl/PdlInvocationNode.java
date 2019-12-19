// Copyright 2019 Sam Shen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

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
