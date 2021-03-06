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
