package rpl;

public class EvalException extends RplException {

	private static final long serialVersionUID = 1L;

	public EvalException(String message) {
		super(message);
	}

	public EvalException(String message, Throwable t) {
		super(message, t);
	}

}
