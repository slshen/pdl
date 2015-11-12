package rpl;

public abstract class RplNode {
	private final ThreadLocal<Object> data = new ThreadLocal<>();
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

	public int getCol() {
		return col;
	}

	public void setCol(int col) {
		this.col = col;
	}

	void setLocation(RplParser parser) {
		source = parser.getSource();
		line = parser.getLine();
		col = parser.getColumn();
	}

	public Object getData() {
		return data.get();
	}

	public void setData(Object data) {
		this.data.set(data);
	}

}
