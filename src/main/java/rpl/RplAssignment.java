package rpl;

import java.util.ArrayList;
import java.util.List;

public class RplAssignment {
	
	private final String name;
	private final List<RplConditionalAssignment> assignments = new ArrayList<>();

	public RplAssignment(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public List<RplConditionalAssignment> getAssignments() {
		return assignments;
	}

}
