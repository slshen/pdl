package rpl;

public class RplException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private String source;
	private int line;
	private int column;

	public RplException(String message, Throwable cause) {
		super(message, cause);
	}

	public RplException(String message) {
		super(message);
	}

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
		return column;
	}

	public void setColumn(int column) {
		this.column = column;
	}

}
