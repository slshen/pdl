package pdl;

import java.util.ArrayList;
import java.util.List;

public class PdlAssignment {
	
	private final String name;
	private final List<PdlConditionalAssignment> conditionalAssignments = new ArrayList<>();

	public PdlAssignment(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<PdlConditionalAssignment> getConditionalAssignments() {
		return conditionalAssignments;
	}

}
