package pdl;

public class EvalException extends PdlException {

	private static final long serialVersionUID = 1L;

	public EvalException(String message) {
		super(message);
	}

	public EvalException(String message, Throwable t) {
		super(message, t);
	}

}
