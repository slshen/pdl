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

import java.util.LinkedHashMap;
import java.util.Map;

public class PdlDictNode extends PdlExpressionNode {

	private final Map<Object, Object> dict = new LinkedHashMap<>();
	private boolean set;

	@Override
	public void accept(PdlExpressionNodeVisitor visitor) {
		visitor.visit(this);
	}

	/**
	 * For sets, {@link Map#keySet()} contains elements, otherwise this is a map
	 * from {@link String} to {@link PdlExpressionNode}.
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
