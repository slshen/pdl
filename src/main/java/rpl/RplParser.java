package rpl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

public class RplParser {

	private final Map<String, RplAssignment> assignments = new LinkedHashMap<>();
	private final List<RplExpressionNode> conditions = new ArrayList<>();
	private String source;
	private PushbackTokenizer tokenizer;

	public RplParser() {
	}

	@VisibleForTesting
	Map<String, RplAssignment> getAssignments() {
		return assignments;
	}

	public RplScope getResult() {
		return new RplScope(new LinkedHashMap<>(assignments));
	}

	/*
	 * Grammar:
	 * 
	 * block = (assignment | conditional_block)*
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
			parseBlock(true);
		} finally {
			tokenizer = null;
			source = null;

		}
	}

	private void parseBlock(boolean top) throws IOException {
		int t;
		while ((t = tokenizer.nextToken()) != Tokenizer.EOF) {
			if (t == Tokenizer.ID) {
				if (tokenizer.getTokenValue().equals("if")) {
					parseConditionalBlock();
				} else {
					parseAssignment();
				}
			} else if (!top && t == '}') {
				tokenizer.pushback();
				break;
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
		parseBlock(false);
		if (tokenizer.nextToken() != '}') {
			throw syntaxError("expecting '}' to end conditional block");
		}
		conditions.remove(conditions.size() - 1);
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
		if (t == Tokenizer.COLON_EQ) {
			cond.setOverride(true);
		} else if (t == Tokenizer.PLUS_EQ) {
			cond.setAppend(true);
		}
		assignment.getAssignments().add(cond);
	}

	private RplBinaryOperatorNode createBinaryOperatorNode(RplExpressionNode left, int operator) throws IOException {
		RplBinaryOperatorNode op = new RplBinaryOperatorNode();
		op.setLocation(this);
		op.setLeft(left);
		op.setOperator(operator);
		return op;
	}

	private RplUnaryOperatorNode createUnaryOperatorNode(int operator) {
		RplUnaryOperatorNode node = new RplUnaryOperatorNode();
		node.setLocation(this);
		node.setOperator(operator);
		return node;
	}

	/*
	 * expression = and_test ('||' and_test)*
	 */
	private RplExpressionNode parseExpression() throws IOException {
		RplExpressionNode expression = parseAndTest();
		while (tokenizer.nextToken() == Tokenizer.L_OR) {
			expression = createBinaryOperatorNode(expression, Tokenizer.L_OR).withRight(parseExpression());
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * and_test = not_test ("&&" not_test)*
	 */
	private RplExpressionNode parseAndTest() throws IOException {
		RplExpressionNode expression = parseNotTest();
		if (tokenizer.nextToken() == Tokenizer.L_AND) {
			expression = createBinaryOperatorNode(expression, Tokenizer.L_AND).withRight(parseAndTest());
		} else {
			tokenizer.pushback();
		}
		return expression;
	}

	/*
	 * not_test: '!' not_test | comparison
	 */
	private RplExpressionNode parseNotTest() throws IOException {
		if (tokenizer.nextToken() == '!') {
			return createUnaryOperatorNode('!').withTarget(parseNotTest());
		} else {
			tokenizer.pushback();
			return parseComparison();
		}
	}

	/*
	 * comparison: expr (comp_op expr)*
	 */
	private RplExpressionNode parseComparison() throws IOException {
		RplExpressionNode expression = parseExpr();
		int t = tokenizer.nextToken();
		while (t == Tokenizer.EQ || t == Tokenizer.GE || t == Tokenizer.LE || t == Tokenizer.NEQ || t == '>'
				|| t == '<') {
			expression = createBinaryOperatorNode(expression, t).withRight(parseComparison());
			t = tokenizer.nextToken();
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * xor_expr: and_expr ('^' and_expr)*
	 */
	private RplExpressionNode parseExpr() throws IOException {
		RplExpressionNode expression = parseAndExpr();
		while (tokenizer.nextToken() == '^') {
			expression = createBinaryOperatorNode(expression, '^').withRight(parseAndExpr());
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * and_expr: shift_expr ('&' shift_expr)*
	 */
	private RplExpressionNode parseAndExpr() throws IOException {
		RplExpressionNode expression = parseShiftExpr();
		while (tokenizer.nextToken() == '&') {
			expression = createBinaryOperatorNode(expression, '&').withRight(parseShiftExpr());
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * shift_expr: arith_expr (('<<'|'>>') arith_expr)*
	 */
	private RplExpressionNode parseShiftExpr() throws IOException {
		RplExpressionNode expression = parseArithExpr();
		int t = tokenizer.nextToken();
		while (t == Tokenizer.L_SHIFT || t == Tokenizer.R_SHIFT) {
			expression = createBinaryOperatorNode(expression, t).withRight(parseArithExpr());
			t = tokenizer.nextToken();
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * arith_expr: term (('+'|'-') term)*
	 */
	private RplExpressionNode parseArithExpr() throws IOException {
		RplExpressionNode expression = parseTerm();
		int t = tokenizer.nextToken();
		while (t == '+' || t == '-') {
			expression = createBinaryOperatorNode(expression, t).withRight(parseTerm());
			t = tokenizer.nextToken();
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * term: factor (('*'|'/'|'%') factor)*
	 */
	private RplExpressionNode parseTerm() throws IOException {
		RplExpressionNode expression = parseFactor();
		int t = tokenizer.nextToken();
		while (t == '*' || t == '/' || t == '%') {
			expression = createBinaryOperatorNode(expression, t).withRight(parseFactor());
			t = tokenizer.nextToken();
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * factor: ('+'|'-'|'~') factor | primary
	 */
	private RplExpressionNode parseFactor() throws IOException {
		int t = tokenizer.nextToken();
		if (t == '+' || t == '-' || t == '~') {
			return createUnaryOperatorNode(t).withTarget(parseFactor());
		} else {
			tokenizer.pushback();
			return parsePrimary();
		}
	}

	/*
	 * primary: atom trailer*
	 * 
	 * 
	 * 
	 * argument_list = expression ("," expression)*
	 */
	private RplExpressionNode parsePrimary() throws IOException {
		RplExpressionNode expression = parseAtom();
		while (true) {
			RplExpressionNode trailer = parseTrailer(expression);
			if (trailer == expression) {
				break;
			}
			expression = trailer;
		}
		return expression;
	}

	/*
	 * atom: ('(' expression ')' | '[' [listmaker] ']' | '{' [dictorsetmaker]
	 * '}' | id | NUMBER | STRING+)
	 */
	private RplExpressionNode parseAtom() throws IOException {
		int t = tokenizer.nextToken();
		RplExpressionNode expression;
		if (t == '(') {
			expression = parseExpression();
			if (tokenizer.nextToken() != ')') {
				throw syntaxError("invalid parenthesized expression");
			}
		} else if (t == '[') {
			RplListNode node = new RplListNode();
			node.setLocation(this);
			parseExpressionList(']', node.getElements());
			expression = node;
		} else if (t == Tokenizer.ID) {
			RplGetValueNode node = new RplGetValueNode();
			node.setLocation(this);
			node.setName(tokenizer.getTokenValue());
			expression = node;
		} else if (t == Tokenizer.NUMBER || t == Tokenizer.STRING || t == Tokenizer.INTERP_STRING) {
			RplConstantNode node = new RplConstantNode();
			node.setLocation(this);
			node.setValue(tokenizer.getTokenValue());
			expression = node;
		} else {
			throw syntaxError(String.format("unexpected token %c", (char) t));
		}
		return expression;
	}

	/*
	 * trailer: '[' subscriptlist ']' | '.' NAME ('(' [arglist] ')')?
	 */
	private RplExpressionNode parseTrailer(RplExpressionNode expression) throws IOException {
		int t = tokenizer.nextToken();
		if (t == '.') {
			t = tokenizer.nextToken();
			if (t != Tokenizer.ID) {
				throw syntaxError("must be attribute name");
			}
			String name = tokenizer.getTokenValue();
			t = tokenizer.nextToken();
			if (t == '(') {
				RplInvocationNode invocationNode = new RplInvocationNode();
				invocationNode.setLocation(this);
				invocationNode.setMethodName(name);
				invocationNode.setTarget(expression);
				parseExpressionList(')', invocationNode.getArguments());
				expression = invocationNode;
			} else {
				RplAttributeNode node = new RplAttributeNode();
				node.setBase(expression);
				node.setAttributeName(name);
				expression = node;
				tokenizer.pushback();
			}
		} else if (t == '[') {
			RplSubscriptNode node = new RplSubscriptNode();
			node.setLocation(this);
			node.setTarget(expression);
			node.setIndex(parseExpression());
			t = tokenizer.nextToken();
			if (t != ']') {
				throw syntaxError("expecting ] in subscript");
			}
			expression = node;
		} else {
			tokenizer.pushback();
		}
		return expression;
	}

	private void parseExpressionList(int terminal, List<RplExpressionNode> args) throws IOException {
		int t = tokenizer.nextToken();
		while (t != terminal) {
			if (t == Tokenizer.EOF) {
				throw syntaxError("unexpected EOF in argument list");
			}
			tokenizer.pushback();
			args.add(parseExpression());
			t = tokenizer.nextToken();
			if (t == ',' || t == terminal) {
				continue;
			}
			throw syntaxError("xpecting either ',' or terminator in list");
		}
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
