package rpl;

import java.util.ArrayList;
import java.util.List;

public class RplAssignment {
	
	private final String name;
	private final List<RplConditionalAssignment> conditionalAssignments = new ArrayList<>();

	public RplAssignment(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<RplConditionalAssignment> getConditionalAssignments() {
		return conditionalAssignments;
	}

}
