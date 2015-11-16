package rpl;

public class SyntaxException extends RplException {

	private static final long serialVersionUID = 1L;
	private boolean eof;

	public SyntaxException(String message, Throwable cause) {
		super(message, cause);
	}

	public SyntaxException(String message) {
		super(message);
	}

	public boolean isEof() {
		return eof;
	}

	public void setEof(boolean eof) {
		this.eof = eof;
	}

}
