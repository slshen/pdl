package pdl;

public class PdlException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private String source;
	private int line;
	private int column;

	public PdlException(String message, Throwable cause) {
		super(message, cause);
	}

	public PdlException(String message) {
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
