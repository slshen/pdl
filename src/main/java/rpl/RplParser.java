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
	
	public RplScope getResult() {
		return new RplScope(new LinkedHashMap<>(assignments));
	}
	
	@VisibleForTesting
	Map<String, RplAssignment> getAssignments() { return assignments; }

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

	/*
	 * expression = and_test | expression "||" and_test
	 *
	 * expr: xor_expr ('|' xor_expr)*
	 */
	private RplExpressionNode parseExpression() throws IOException {
		RplExpressionNode expression = parseAndTest();
		if (tokenizer.nextToken() == Tokenizer.L_OR) {
			expression = createBinaryOperatorNode(expression, Tokenizer.L_OR).withRight(parseExpression());
		} else {
			tokenizer.pushback();
		}
		return expression;
	}

	private RplBinaryOperatorNode createBinaryOperatorNode(RplExpressionNode left, int operator) throws IOException {
		RplBinaryOperatorNode op = new RplBinaryOperatorNode();
		op.setLocation(this);
		op.setLeft(left);
		op.setOperator(operator);
		return op;
	}

	/*
	 * and_test = not_test | and_test "&&" not_test
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

	private RplUnaryOperatorNode createUnaryOperatorNode(int operator) {
		RplUnaryOperatorNode node = new RplUnaryOperatorNode();
		node.setLocation(this);
		node.setOperator(operator);
		return node;
	}

	/*
	 * comparison: expr (comp_op expr)*
	 */
	private RplExpressionNode parseComparison() throws IOException {
		RplExpressionNode expression = parseExpr();
		int t = tokenizer.nextToken();
		if (t == Tokenizer.EQ || t == Tokenizer.GE || t == Tokenizer.LE || t == Tokenizer.NEQ) {
			return createBinaryOperatorNode(expression, t).withRight(parseComparison());
		} else {
			tokenizer.pushback();
			return expression;
		}
	}

	/*
	 * xor_expr: and_expr ('^' and_expr)*
	 */
	private RplExpressionNode parseExpr() throws IOException {
		RplExpressionNode expression = parseAndExpr();
		if (tokenizer.nextToken() == '^') {
			return createBinaryOperatorNode(expression, '^').withRight(parseAndExpr());
		} else {
			tokenizer.pushback();
			return expression;
		}
	}

	/*
	 * and_expr: shift_expr ('&' shift_expr)*
	 */
	private RplExpressionNode parseAndExpr() throws IOException {
		RplExpressionNode expression = parseShiftExpr();
		if (tokenizer.nextToken() == '&') {
			return createBinaryOperatorNode(expression, '&').withRight(parseShiftExpr());
		} else {
			tokenizer.pushback();
			return expression;
		}
	}

	/*
	 * shift_expr: arith_expr (('<<'|'>>') arith_expr)*
	 */
	private RplExpressionNode parseShiftExpr() throws IOException {
		RplExpressionNode expression = parseArithExpr();
		int t = tokenizer.nextToken();
		if (t == Tokenizer.L_SHIFT || t == Tokenizer.R_SHIFT) {
			return createBinaryOperatorNode(expression, t).withRight(parseArithExpr());
		} else {
			tokenizer.pushback();
			return expression;
		}
	}

	/*
	 * arith_expr: term (('+'|'-') term)*
	 */
	private RplExpressionNode parseArithExpr() throws IOException {
		RplExpressionNode expression = parseTerm();
		int t = tokenizer.nextToken();
		if (t == '+' || t == '-') {
			return createBinaryOperatorNode(expression, t).withRight(parseTerm());
		} else {
			tokenizer.pushback();
			return expression;
		}
	}

	/*
	 * term: factor (('*'|'/'|'%') factor)*
	 */
	private RplExpressionNode parseTerm() throws IOException {
		RplExpressionNode expression = parseFactor();
		int t = tokenizer.nextToken();
		if (t == '*' || t == '/' || t == '%') {
			return createBinaryOperatorNode(expression, t).withRight(parseFactor());
		} else {
			tokenizer.pushback();
			return expression;
		}
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
	 * primary: atom | attributeref | call | '(' expression ')'
	 * 
	 * atom: id | constant
	 * 
	 * attributeref: primary "." id
	 * 
	 * call = "call" string "(" argument_list ")" | "new" string "("
	 * argument_list ")"
	 * 
	 * argument_list = expression ("," expression)*
	 */
	private RplExpressionNode parsePrimary() throws IOException {
		int t = tokenizer.nextToken();
		RplExpressionNode expression;
		if (t == Tokenizer.ID) {
			if (tokenizer.getTokenValue().equals("call") || tokenizer.getTokenValue().equals("new")) {
				expression = parseInvocation();
			} else {
				RplGetValueNode node = new RplGetValueNode();
				node.setLocation(this);
				node.setName(tokenizer.getTokenValue());
				expression = node;
			}
		} else if (t == '(') {
			expression = parseExpression();
			if (tokenizer.nextToken() != ')') {
				throw syntaxError("invalid parenthesized expression");
			}
		} else if (t == Tokenizer.NUMBER || t == Tokenizer.STRING || t == Tokenizer.INTERP_STRING) {
			RplConstantNode node = new RplConstantNode();
			node.setLocation(this);
			node.setValue(tokenizer.getTokenValue());
			expression= node;
		} else {
			throw syntaxError(String.format("unexpected token %c", (char) t));
		}
		t = tokenizer.nextToken();
		if (t == '.') {
			t = tokenizer.nextToken();
			if (t != Tokenizer.ID) {
				throw syntaxError("must be attribute name");
			}
			RplAttributeNode node = new RplAttributeNode();
			node.setBase(expression);
			node.setAttributeName(tokenizer.getTokenValue());
			expression = node;
		} else {
			tokenizer.pushback();
		}
		return expression;
	}

	private RplExpressionNode parseInvocation() throws IOException {
		RplInvocationNode node = new RplInvocationNode();
		node.setLocation(this);
		node.setConstructor(tokenizer.getTokenValue().equals("new"));
		RplExpressionNode target = parseExpression();
		node.setTarget(target);
		int t = tokenizer.nextToken();
		if (t != '(') {
			tokenizer.pushback();
			throw syntaxError("expecting '('");
		}
		t = tokenizer.nextToken();
		while (t != ')') {
			if (t == Tokenizer.EOF) {
				throw syntaxError("unexpected EOF in argument list");
			}
			tokenizer.pushback();
			node.getArguments().add(parseExpression());
			t = tokenizer.nextToken();
			if (t == ',' || t == ')') {
				continue;
			}
			throw syntaxError("expecting either ',' or ')' in argument list");
		}
		return node;
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
