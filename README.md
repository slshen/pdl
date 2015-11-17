# Recursive Property Language

RPL is a little language for defining properties:

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

## Property Definitions

A property may be defined in one of the following ways:

1. Simple assignment in the form `PROPERTY_NAME = expression`.

2. Appending assignment in the form `PROPERTY_NAME += expression`.

3. Conditional assignments, in the form `if (expression) { assignments ... }`

4. Property sets, explained below.



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

## Expressions

Property names are always strings.  Property values may be any java type.

Literal values are always treated as strings