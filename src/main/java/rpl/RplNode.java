package rpl;

public abstract class RplNode {
	private String source;
	private int line, col;

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public int getColumn() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}
	
	public String diagMessage(String msg) {
		return String.format("%s:%d:%s", getSource(), getLine(), msg);
	}

}
