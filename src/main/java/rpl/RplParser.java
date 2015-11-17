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

	private <T extends RplNode> T create(T node) {
		node.setSource(getSource());
		node.setLine(getLine());
		node.setCol(getColumn());
		return node;
	}

	/*
	 * Grammar:
	 * 
	 * block = (assignment | conditional_block)*
	 * 
	 * conditional_block = "if" "(" expression ")" "{" block* "}"
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

	/*
	 * assignment = id ("="|":="|"+=") (expression | property_set)
	 */
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
		RplConditionalAssignment cond = create(new RplConditionalAssignment());
		cond.getConditions().addAll(conditions);
		if (t == Tokenizer.COLON_EQ) {
			cond.setOverride(true);
		} else if (t == Tokenizer.PLUS_EQ) {
			cond.setAppend(true);
		}
		t = tokenizer.nextToken();
		if (t == '{') {
			cond.setPropertySet(parsePropertySet());
		} else {
			tokenizer.pushback();
			cond.setValue(parseExpression());
		}

		assignment.getConditionalAssignments().add(cond);
	}

	/*
	 * property_set = '{' property_def (',' property_def)* '}'
	 * 
	 * property_def = ID ':' expression
	 */
	private RplPropertySetNode parsePropertySet() throws IOException {
		int t = tokenizer.nextToken();
		if (t != Tokenizer.ID) {
			throw syntaxError("property set must be in form 'ID: expression'");
		}
		RplPropertySetNode propertySet = create(new RplPropertySetNode());
		while (true) {
			String name = tokenizer.getTokenValue();
			t = tokenizer.nextToken();
			if (t != ':') {
				throw syntaxError("property name must be followed by ':'");
			}
			RplExpressionNode expression = parseExpression();
			propertySet.getProperties().put(name, expression);
			t = tokenizer.nextToken();
			if (t == '}') {
				break;
			} else if (t != ',') {
				throw syntaxError("property definitions must be separated by ','");
			}
			t = tokenizer.nextToken();
		}
		return propertySet;
	}

	private RplBinaryOperatorNode createBinaryOperatorNode(RplExpressionNode left, int operator) throws IOException {
		RplBinaryOperatorNode op = create(new RplBinaryOperatorNode());
		op.setLeft(left);
		op.setOperator(operator);
		return op;
	}

	private RplUnaryOperatorNode createUnaryOperatorNode(int operator) {
		RplUnaryOperatorNode node = create(new RplUnaryOperatorNode());
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
		while (t == Tokenizer.EQ || t == Tokenizer.GE || t == Tokenizer.LE || t == Tokenizer.NEQ || t == '>' || t == '<'
				|| t == Tokenizer.ID) {
			int operator = t;
			if (t == Tokenizer.ID) {
				if (tokenizer.getTokenValue().equals("in")) {
					operator = RplBinaryOperatorNode.IN;
				} else if (tokenizer.getTokenValue().equals("not")) {
					if (tokenizer.nextToken() == Tokenizer.ID && tokenizer.getTokenValue().equals("in")) {
						operator = RplBinaryOperatorNode.NOT_IN;
					} else {
						tokenizer.pushback();
						break;
					}
				} else {
					break;
				}
			}
			expression = createBinaryOperatorNode(expression, operator).withRight(parseComparison());
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
	 * primary: (ctor | atom) trailer*
	 */
	private RplExpressionNode parsePrimary() throws IOException {
		RplExpressionNode expression;
		int t = tokenizer.nextToken();
		if (t == Tokenizer.ID && tokenizer.getTokenValue().equals("new")) {
			expression = parseCtor();
		} else {
			tokenizer.pushback();
			expression = parseAtom();
		}
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
	 * ctor: 'new' ID ('.' ID)* '(' expression_list ')'
	 */
	private RplExpressionNode parseCtor() throws IOException {
		RplInvocationNode node = create(new RplInvocationNode());
		node.setConstructor(true);
		int t = tokenizer.nextToken();
		if (t != Tokenizer.ID) {
			throw syntaxError("'new' must be followed by a type name");
		}
		StringBuilder typeName = new StringBuilder(tokenizer.getTokenValue());
		while ((t = tokenizer.nextToken()) == '.') {
			t = tokenizer.nextToken();
			if (t != Tokenizer.ID) {
				throw syntaxError("invalid type name for 'new'");
			}
			typeName.append('.').append(tokenizer.getTokenValue());
		}
		node.setMethodName(typeName.toString());
		if (t != '(') {
			throw syntaxError("'new' operator must have constructor arguments");
		}
		parseExpressionList(')', node.getArguments());
		return node;
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
			RplListNode node = create(new RplListNode());
			parseExpressionList(']', node.getElements());
			expression = node;
		} else if (t == '{') {
			RplDictNode node = create(new RplDictNode());
			parseDictEntries(node);
			expression = node;
		} else if (t == Tokenizer.NUMBER || t == Tokenizer.STRING || t == Tokenizer.INTERP_STRING || (t == Tokenizer.ID
				&& (tokenizer.getTokenValue().equals("true") || tokenizer.getTokenValue().equals("false")))) {
			RplConstantNode node = create(new RplConstantNode());
			node.setValue(tokenizer.getTokenValue());
			expression = node;
		} else if (t == Tokenizer.ID) {
			RplGetValueNode node = create(new RplGetValueNode());
			node.setName(tokenizer.getTokenValue());
			expression = node;
		} else {
			throw syntaxError(String.format("unexpected token %c", (char) t));
		}
		return expression;
	}

	/*
	 * dict_entries = dict_entry (',' dict_entry)*
	 * 
	 *  dict_entry = (( ID | STRING )
	 * ':' expression)
	 */
	private void parseDictEntries(RplDictNode node) throws IOException {
		int t = tokenizer.nextToken();
		if (t == Tokenizer.ID || t == Tokenizer.STRING) {
			// peek ahead to see if this is a set or dict
			int t2 = tokenizer.nextToken();
			if (t2 != ':') {
				node.setSet(true);
			}
			tokenizer.pushback();
		} else if (t != '}') {
			node.setSet(true);
		}
		tokenizer.pushback();
		while (true) {
			t = tokenizer.nextToken();
			if (t == '}')
				break;
			if (node.isSet()) {
				tokenizer.pushback();
				node.getDict().put(parseExpression(), Boolean.TRUE);
			} else {
				// dict
				if (t == Tokenizer.ID || t == Tokenizer.STRING) {
					String name = tokenizer.getTokenValue();
					t = tokenizer.nextToken();
					if (t != ':') {
						throw syntaxError("dictionary entry must be name : value");
					}
					RplExpressionNode value = parseExpression();
					node.getDict().put(name, value);
				}
			}
			t = tokenizer.nextToken();
			if (t == ',') {
				continue;
			}
			if (t == '}')
				break;
		}
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
				RplInvocationNode invocationNode = create(new RplInvocationNode());
				invocationNode.setMethodName(name);
				invocationNode.setTarget(expression);
				parseExpressionList(')', invocationNode.getArguments());
				expression = invocationNode;
			} else {
				RplAttributeNode node = new RplAttributeNode();
				node.setTarget(expression);
				node.setAttributeName(name);
				expression = node;
				tokenizer.pushback();
			}
		} else if (t == '[') {
			RplSubscriptNode node = create(new RplSubscriptNode());
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

	/*
	 * expression_list = expression ("," expression)*
	 */
	private void parseExpressionList(int terminal, List<RplExpressionNode> args) throws IOException {
		int t = tokenizer.nextToken();
		if (t == terminal) {
			return;
		}
		tokenizer.pushback();
		while (true) {
			args.add(parseExpression());
			t = tokenizer.nextToken();
			if (t == terminal) {
				break;
			}
			if (t != ',') {
				throw syntaxError("expecting either ',' or terminator in list");
			}
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
		SyntaxException e = new SyntaxException(String.format("%s:%d %s", source, tokenizer.getLine(), string));
		e.setSource(source);
		e.setLine(tokenizer.getLine());
		e.setColumn(tokenizer.getColumn());
		e.setEof(tokenizer.getToken() == Tokenizer.EOF);
		return e;
	}

}
