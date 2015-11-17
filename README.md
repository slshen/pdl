# Recursive Property Language

RPL is a little language for defining properties:

    # both shell-style line comments
    // and java-style line comments
    /*
     * and block comments are ok
     */
    GREETING = "hello"
    SUBJECT = "world"

Properties can be defined in terms of other properties:

    SAY = GREETING + ", " + SUBJECT

Property definitions can be conditional (including nested conditions), and can
replace the definition of a property, or append values to a property.

    if (IS_NOISY) {
      GREETING = "(shouting) hello"
      SAY += "!"
    }

Property definitions are declarative.  Property values are not evaluated
and assigned in order.  Instead, properties are evaluated as-needed
recursively.

Rule order is important, later rules take precedence over earlier rules.

## Property Definitions

A property may be defined in one of the following ways:

1. Simple assignment in the form `PROPERTY_NAME = expression`.

2. Appending assignment in the form `PROPERTY_NAME += expression`.
Appending works as follows: if both values are numbers (or can be
parsed as numbers), the result is the sum; if the existing value is
null the result is the new value; if the existing value is a
collection, the new values are added to the collection; if the
existing value is a map, the new value must also be a map, and the map
is extended with the new values.  Otherwise, the string values are
appended.

3. Overriding assignment in the form `PROPERTY_NAME := expression`.  Sets
the value of a property, and does not evaluate any more rules.

3. Conditional assignments, in the form `if (expression) { assignments ... }`.
An expression is true if it's equal to the literal string "true", or is 
`Boolean.TRUE`, or is a non-zero number, or is a non-empty collection or
map.

4. Property sets, explained below.

## Expressions

Literal values may be:

* Strings, in either `'single'` or `"double"` quotes, or in python-style `"""triple"""` quotes.

* Numbers

* The literals `true` or `false`.

* A literal list may be written with `[` and `]`.

* A literal map may be written as `{ name: expression }`.  The `name` may be enclosed in quotes.

* A literal set may be written as `{ expression , ... }`.

Most java operators are supported:

* The binary operators `+ - * / % << >> | & ^` are supported.  For `+` the
rules are the same of the `+=` assignment operator described above.
For `-`, if the left value is a collection, the right value or values
are removed; if the left value is a map, the right value or values are
removed; otherwise it's the arithmetic difference of the two values.

* The comparison operators `== != >= <=`.  In addition, `in` and `not in`
can be used for membership testing.

* The unary operators `! ~ + -`.

* The short-cut logical `&&` and `||` operators work as in java.

Java methods may be invoked on values.  (N.B. -- no current syntax to invoke a
static method.)

New java objects can be created with `new class-name( arguments ... )`.  The
class name must be fully qualified unless it's in `java.lang` or `java.util`.

## Property Sets

Property sets are sets of properties:

    oracle_jdbc_template = {
        JDBC_URL = "jdbc:oracle:thin:@" + host + ":" + port + "/" + service
    }

So that `oracle_jdbc_template.jdbc` evaluates an oracle jdbc URL.  The evaluation
takes place in the content of the property set -- names in the set take precedence
over global names.

Property sets can extended:

    if (ENV == "dev") {
       oracle_jdbc_template += {
           host = "oracledev-" + APP_ID + "example.com"
           port = 1521
           service = "dev"
       }
    }

In this case, we establish some defaults if we're running in `dev`.

Property sets may be assigned to other properties.

    if (DB_TYPE == "oracle") {
        DB = oracle_jdbc_template
    }

Here we define a new property set, that contains all the property definitions of
`oracle_jdbc_template`.  Note that properties can now be added or to either `DB`
or `oracle_jdbc_template`.

