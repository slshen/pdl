# Recursive Property Language

Like a properties file, in that we can define properties:

    GREETING = "hello"
    SUBJECT = "world"

Except we can define properties in terms of other properties:

    SAY = GREETING + ", " + SUBJECT
    
We can also introduce conditions:

    if (ENV == 'dev') {
      GREETING = "(shouting) hello"
    }
    if (ENV == 'prod') {
      GREETING = "(whispering) hello"
    }

Conditions can be nested:

    if (ENV != "prod") {
      AUTH_SERVICE = "https://nonprod-auth.example.com/"
      WORKFLOW_SERVICE = "https://nonprod-wf.example.com/"
      if (ENV == "demo") {
         WORKFLOW_SERVICE = "https://demo-wf.example.com/"
      }
    }

Unlike in most programmig languages, properties are not evaluated
and assigned in order.  Instead, properties are evaluated as-needed
recursively.  For example:

    Z = X + 1
    if (Y == 0) {
       Z = X + 2
    }
    if (Y == 1) {
       Z = X * 2
    }
    X = Y + 1

To evaulate `Z`, when `Y = 1`

1. Calculate `X + 1`, which is `Y + 1`, which is 2.  This is our
intitial, temporary answer for `Z`.
2. See if `Y == 0` -- it isn't so stick with 2.
3. See if `Y == 1` -- it is, so evaluate `X * 2` which is 4.  This is
our new temporary answer for `Z`.
4. There are no further assignments to `Z`, so the final answer is 4.
