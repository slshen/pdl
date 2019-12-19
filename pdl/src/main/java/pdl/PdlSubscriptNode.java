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

public class PdlSubscriptNode extends PdlExpressionNode {

	private PdlExpressionNode target;
	private PdlExpressionNode index;

	public PdlExpressionNode getTarget() {
		return target;
	}

	public void setTarget(PdlExpressionNode target) {
		this.target = target;
	}

	public PdlExpressionNode getIndex() {
		return index;
	}

	public void setIndex(PdlExpressionNode index) {
		this.index = index;
	}

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

}
