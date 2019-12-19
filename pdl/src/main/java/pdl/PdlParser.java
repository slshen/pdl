// Copyright 2019 Sam Shen
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package pdl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PdlParser parses a series of input sources in the 
 * <a href="https://github.com/slshen/pdl">PDL language</a>.
 * 
 * @author samshen
 */
public class PdlParser {

	private final Map<String, PdlAssignment> assignments = new LinkedHashMap<>();
	private final List<PdlExpressionNode> conditions = new ArrayList<>();
	private String source;
	private PushbackTokenizer tokenizer;

	public PdlParser() {
	}

	//@VisibleForTesting
	Map<String, PdlAssignment> getAssignments() {
		return assignments;
	}

	/**
	 * Returns the top-level scope of all parsed language statements
	 * so far.
	 */
	public PdlScope getResult() {
		return new PdlScope(new LinkedHashMap<>(assignments));
	}

	private <T extends PdlNode> T create(T node) {
		node.setSource(getSource());
		node.setLine(getLine());
		node.setCol(getColumn());
		return node;
	}

	/**
	 * Parses a new source of language statements.  May be called multiple
	 * times.
	 * 
	 * @param in the textual source of of the statements
	 * @param filename the name of the source for diagnostic purposes
	 *   (see {@link PdlDiag}).  May be null.
	 * @throws IOException
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

	/*
	 * Grammar:
	 * 
	 * block = (assignment | conditional_block)*
	 * 
	 * conditional_block = "if" "(" expression ")" "{" block* "}"
	 */
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
		PdlExpressionNode expression = parseExpression();
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
		PdlAssignment assignment = assignments.get(tokenizer.getTokenValue());
		if (assignment == null) {
			assignment = new PdlAssignment(tokenizer.getTokenValue());
			assignments.put(assignment.getName(), assignment);
		}
		int t = tokenizer.nextToken();
		if (t != '=' && t == Tokenizer.COLON_EQ && t == Tokenizer.PLUS_EQ) {
			throw syntaxError("expecting an assignment operator");
		}
		PdlConditionalAssignment cond = create(new PdlConditionalAssignment(assignment.getName()));
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
	 * property_def = ID '=' expression
	 */
	private PdlPropertySetNode parsePropertySet() throws IOException {
		int t = tokenizer.nextToken();
		if (t != Tokenizer.ID) {
			throw syntaxError("property set must be in form 'ID = expression'");
		}
		PdlPropertySetNode propertySet = create(new PdlPropertySetNode());
		while (true) {
			String name = tokenizer.getTokenValue();
			t = tokenizer.nextToken();
			if (t != '=') {
				throw syntaxError("property name must be followed by '='");
			}
			PdlExpressionNode expression = parseExpression();
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

	private PdlBinaryOperatorNode createBinaryOperatorNode(PdlExpressionNode left, int operator) throws IOException {
		PdlBinaryOperatorNode op = create(new PdlBinaryOperatorNode());
		op.setLeft(left);
		op.setOperator(operator);
		return op;
	}

	private PdlUnaryOperatorNode createUnaryOperatorNode(int operator) {
		PdlUnaryOperatorNode node = create(new PdlUnaryOperatorNode());
		node.setOperator(operator);
		return node;
	}

	/*
	 * expression = and_test ('||' and_test)*
	 */
	private PdlExpressionNode parseExpression() throws IOException {
		PdlExpressionNode expression = parseAndTest();
		while (tokenizer.nextToken() == Tokenizer.L_OR) {
			expression = createBinaryOperatorNode(expression, Tokenizer.L_OR).withRight(parseExpression());
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * and_test = not_test ("&&" not_test)*
	 */
	private PdlExpressionNode parseAndTest() throws IOException {
		PdlExpressionNode expression = parseNotTest();
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
	private PdlExpressionNode parseNotTest() throws IOException {
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
	private PdlExpressionNode parseComparison() throws IOException {
		PdlExpressionNode expression = parseExpr();
		int t = tokenizer.nextToken();
		while (t == Tokenizer.EQ || t == Tokenizer.GTE || t == Tokenizer.LTE || t == Tokenizer.NEQ || t == '>' || t == '<'
				|| t == Tokenizer.ID) {
			int operator = t;
			if (t == Tokenizer.ID) {
				if (tokenizer.getTokenValue().equals("in")) {
					operator = PdlBinaryOperatorNode.IN;
				} else if (tokenizer.getTokenValue().equals("not")) {
					if (tokenizer.nextToken() == Tokenizer.ID && tokenizer.getTokenValue().equals("in")) {
						operator = PdlBinaryOperatorNode.NOT_IN;
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
	 * expr: xor_expr ('|' xor_expr)*
	 *
	 */
	private PdlExpressionNode parseExpr() throws IOException {
		PdlExpressionNode expression = parseXorExpr();
		while (tokenizer.nextToken() == '|') {
			expression = createBinaryOperatorNode(expression, '|').withRight(parseXorExpr());
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * xor_expr: and_expr ('^' and_expr)*
	 */
	private PdlExpressionNode parseXorExpr() throws IOException {
		PdlExpressionNode expression = parseAndExpr();
		while (tokenizer.nextToken() == '^') {
			expression = createBinaryOperatorNode(expression, '^').withRight(parseAndExpr());
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * and_expr: shift_expr ('&' shift_expr)*
	 */
	private PdlExpressionNode parseAndExpr() throws IOException {
		PdlExpressionNode expression = parseShiftExpr();
		while (tokenizer.nextToken() == '&') {
			expression = createBinaryOperatorNode(expression, '&').withRight(parseShiftExpr());
		}
		tokenizer.pushback();
		return expression;
	}

	/*
	 * shift_expr: arith_expr (('<<'|'>>') arith_expr)*
	 */
	private PdlExpressionNode parseShiftExpr() throws IOException {
		PdlExpressionNode expression = parseArithExpr();
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
	private PdlExpressionNode parseArithExpr() throws IOException {
		PdlExpressionNode expression = parseTerm();
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
	private PdlExpressionNode parseTerm() throws IOException {
		PdlExpressionNode expression = parseFactor();
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
	private PdlExpressionNode parseFactor() throws IOException {
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
	private PdlExpressionNode parsePrimary() throws IOException {
		PdlExpressionNode expression;
		int t = tokenizer.nextToken();
		if (t == Tokenizer.ID && tokenizer.getTokenValue().equals("new")) {
			expression = parseCtor();
		} else {
			tokenizer.pushback();
			expression = parseAtom();
		}
		while (true) {
			PdlExpressionNode trailer = parseTrailer(expression);
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
	private PdlExpressionNode parseCtor() throws IOException {
		PdlInvocationNode node = create(new PdlInvocationNode());
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
	private PdlExpressionNode parseAtom() throws IOException {
		int t = tokenizer.nextToken();
		PdlExpressionNode expression;
		if (t == '(') {
			expression = parseExpression();
			if (tokenizer.nextToken() != ')') {
				throw syntaxError("invalid parenthesized expression");
			}
		} else if (t == '[') {
			PdlListNode node = create(new PdlListNode());
			parseExpressionList(']', node.getElements());
			expression = node;
		} else if (t == '{') {
			PdlDictNode node = create(new PdlDictNode());
			parseDictEntries(node);
			expression = node;
		} else if (t == Tokenizer.NUMBER || t == Tokenizer.STRING || t == Tokenizer.INTERP_STRING || (t == Tokenizer.ID
				&& (tokenizer.getTokenValue().equals("true") || tokenizer.getTokenValue().equals("false")))) {
			PdlConstantNode node = create(new PdlConstantNode());
			node.setValue(tokenizer.getTokenValue());
			expression = node;
		} else if (t == Tokenizer.ID) {
			PdlGetValueNode node = create(new PdlGetValueNode());
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
	private void parseDictEntries(PdlDictNode node) throws IOException {
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
					PdlExpressionNode value = parseExpression();
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
	private PdlExpressionNode parseTrailer(PdlExpressionNode expression) throws IOException {
		int t = tokenizer.nextToken();
		if (t == '.') {
			t = tokenizer.nextToken();
			if (t != Tokenizer.ID) {
				throw syntaxError("must be attribute name");
			}
			String name = tokenizer.getTokenValue();
			t = tokenizer.nextToken();
			if (t == '(') {
				PdlInvocationNode invocationNode = create(new PdlInvocationNode());
				invocationNode.setMethodName(name);
				invocationNode.setTarget(expression);
				parseExpressionList(')', invocationNode.getArguments());
				expression = invocationNode;
			} else {
				PdlAttributeNode node = new PdlAttributeNode();
				node.setTarget(expression);
				node.setAttributeName(name);
				expression = node;
				tokenizer.pushback();
			}
		} else if (t == '[') {
			PdlSubscriptNode node = create(new PdlSubscriptNode());
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
	private void parseExpressionList(int terminal, List<PdlExpressionNode> args) throws IOException {
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
