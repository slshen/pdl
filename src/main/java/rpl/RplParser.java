package rpl;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public class RplParser {

	private final Map<String, RplAssignment> assignments = new LinkedHashMap<>();
	private final Queue<RplExpressionNode> conditions = new LinkedList<>();
	private String source;
	private PushbackTokenizer tokenizer;

	public RplParser() {
	}

	/*
	 * Grammar:
	 * 
	 * block = (assignment | conditional_block)*
	 * 
	 * call = "call" string "(" argument_list ")" | "new" string "("
	 * argument_list ")"
	 * 
	 * argument_list = expression ("," expression)*
	 * 
	 * conditional_block = "if" "(" expression ")" "{" block* "}"
	 * 
	 * assignment = id "=" expression | id ":=" expression | id "+=" expression
	 * 
 
	 */
	public void parse(Reader in, String filename) throws IOException {
		try {
			tokenizer = new PushbackTokenizer(new Tokenizer(in));
			source = filename;
			parseBlock();
		} finally {
			tokenizer = null;
			source = null;

		}
	}

	private void parseBlock() throws IOException {
		int t;
		while ((t = tokenizer.nextToken()) != Tokenizer.EOF) {
			if (t == Tokenizer.ID) {
				if (tokenizer.getTokenValue().equals("if")) {
					parseConditionalBlock();
				} else {
					parseAssignment();
				}
			} else {
				throw syntaxError("expecting property assignment or conditional block");
			}
		}
	}

	private void parseConditionalBlock() throws IOException {
		if (tokenizer.nextToken() != '(') {
			throw syntaxError("expecting '(' in if");
		}
		RplExpressionNode expression = parseExpression();
		conditions.add(expression);
		if (tokenizer.nextToken() != ')') {
			throw syntaxError("expending ')' in conditional block");
		}
		if (tokenizer.nextToken() != '{') {
			throw syntaxError("expecting '{' to start conditional block");
		}
		parseBlock();
		if (tokenizer.nextToken() != '}') {
			throw syntaxError("expecting '}' to end conditional block");
		}
	}


	private void parseAssignment() throws IOException {
		RplAssignment assignment = assignments.get(tokenizer.getTokenValue());
		if (assignment == null) {
			assignment = new RplAssignment(tokenizer.getTokenValue());
			assignments.put(assignment.getName(), assignment);
		}
		int t = tokenizer.nextToken();
		if (t != '=' && t == Tokenizer.COLON_EQ && t == Tokenizer.PLUS_EQ) {
			throw syntaxError("expecting an assignment operator");
		}
		RplConditionalAssignment cond = new RplConditionalAssignment();
		cond.setLocation(this);
		cond.getConditions().addAll(conditions);
		cond.setValue(parseExpression());
	}

	/*
	 * expression = and_test | expression "||" and_test
	 *
	 * expr: xor_expr ('|' xor_expr)*
	 * 
	 *
	 * 

	 *
	 */
	private RplExpressionNode parseExpression() throws IOException {
		RplExpressionNode expression = parseAndTest();
		if (tokenizer.nextToken() == Tokenizer.L_OR) {
			expression = createBinaryOperatorNode(expression, Tokenizer.L_OR, parseExpression());
		} else {
			tokenizer.pushback();
		}
		return expression;
	}

	private RplBinaryOperatorNode createBinaryOperatorNode(RplExpressionNode expression, int operator, RplExpressionNode right) throws IOException {
		RplBinaryOperatorNode op = new RplBinaryOperatorNode();
		op.setLocation(this);
		op.setLeft(expression);
		op.setOperator(operator);
		op.setRight(right);
		return op;
	}

	/*
	 * and_test = not_test | and_test "&&" not_test
	 */
	private RplExpressionNode parseAndTest() throws IOException {
		RplExpressionNode expression = parseNotTest();
		if (tokenizer.nextToken() == Tokenizer.L_AND) {
			expression = createBinaryOperatorNode(expression, Tokenizer.L_AND, parseAndTest());
		} else {
			tokenizer.pushback();
		}
		return null;
	}

	/*
	 * not_test: '!' not_test | comparison
	 */
	private RplExpressionNode parseNotTest() throws IOException {
		if (tokenizer.nextToken() == '!') {
			return createUnaryOperatorNode('!', parseNotTest());
		} else {
			tokenizer.pushback();
			return parseComparison();
		}
	}

	private RplExpressionNode createUnaryOperatorNode(int operator, RplExpressionNode target) {
		RplUnaryOperatorNode node = new RplUnaryOperatorNode();
		node.setLocation(this);
		node.setOperator(operator);
		node.setTarget(target);
		return target;
	}

	/*
	 * comparison: expr (comp_op expr)*
	 */
	private RplExpressionNode parseComparison() throws IOException {
		RplExpressionNode expression = parseExpr();
		int t = tokenizer.nextToken();
		if (t == Tokenizer.EQ || t == Tokenizer.GE || t == Tokenizer.LE || t == Tokenizer.NEQ) {
			return createBinaryOperatorNode(expression, t, parseComparison());
		} else {
			tokenizer.pushback();
			return expression;
		}
	}

	/*
	 *  xor_expr: and_expr ('^' and_expr)*
	 */
	private RplExpressionNode parseExpr() throws IOException {
		RplExpressionNode expression = parseAndExpr();
		if (tokenizer.nextToken() == '^') {
			return createBinaryOperatorNode(expression, '^', parseAndExpr());
		} else {
			tokenizer.pushback();
			return expression;
		}
	}

	/*
	 * and_expr: shift_expr ('&' shift_expr)*
	 * 
	 * 
	 * 
	 */
	private RplExpressionNode parseAndExpr() {
		expression = parseShiftExpr();
		return null;
	}

	/*
	 * shift_expr: arith_expr (('<<'|'>>') arith_expr)*
	 * 
	 * 
	 */
	private RplExpressionNode parseShiftExpr() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/*
	 * arith_expr: term (('+'|'-') term)*
	 * 
	 * 
	 */
	private RplExpressionNode parseArithExpr() {
		return null;
	}
	
	/*
	 * term: factor (('*'|'/'|'%') factor)*
	 */
	private RplExpressionNode parseTerm() {
		return null;
	}
	
	/*
	 * factor: ('+'|'-'|'~') factor | primary
	 * 
	 * 
	 */
	private RplExpressionNode parseFactor() {
		return null;
	}
	
	/*
	 * primary: atom | attributeref | call
	 * 
	 * atom: id | constant
	 * 
	 * attributeref: primary "." id
	 */
	private RplExpressionNode parsePrimary() {
		return null;
	}

	String getSource() {
		return source;
	}

	int getLine() {
		return tokenizer.getLine();
	}

	int getColumn() {
		return tokenizer.getColumn();
	}

	private SyntaxException syntaxError(String string) {
		return new SyntaxException(String.format("%s:%d %s", source, tokenizer.getLine(), string));
	}

}
