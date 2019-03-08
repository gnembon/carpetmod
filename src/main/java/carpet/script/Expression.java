/*
 * Copyright 2012-2018 Udo Klimaschewski
 *
 * http://UdoJava.com/
 * http://about.me/udo.klimaschewski
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package carpet.script;

import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import static java.lang.Math.abs;
import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * This Evaluator is initially (very loosely, as of now) based on the following project:
 *
 * EvalEx - Java Expression Evaluator
 *
 * EvalEx is a handy expression evaluator for Java, that
 * allows to evaluate simple mathematical and boolean expressions.
 * For more information, see:
 * <a href="https://github.com/uklimaschewski/EvalEx">EvalEx GitHub
 * repository</a>
 *
 * There is a bunch of operators you can use inside the expression. Those could be considered
 * gneeric type operators
 *
 * <h2>Operator '+'</h2>
 * <p>Allows to add the results of two expressions. If the operands resolve to numbers, the result is
 * arithmetic operation</p>
 * <p>Examples:</p>
 *
 * <p><code> 2+3 =&gt; 5  </code></p>
 * <p><code> 'foo'+3+2 =&gt; 'abc32'  </code></p>
 * <p><code> 3+2+'bar' =&gt; '5bar'  </code></p>
 *
 * <h2>Operator '-'</h2>
 * <p>Allows to add the results of two expressions. If the operands resolve to numbers, the result is
 * arithmetic operation</p>
 *
 * <p>Examples:</p>
 *
 * <p><code> 2+3 =&gt; 5  </code></p>
 * <p><code> 'foo'+3+2 =&gt; 'abc32'  </code></p>
 * <p><code> 3+2+'bar' =&gt; '5bar'  </code></p>
 *
 *
 *
 *
 * loop(num,expr(_),exit(_)?)->value (last)
 * map(list,expr(_,_i), exit(_,_i)) -> list
 * filter(list,expr(_,_i),exit(_,_i)) -> list
 * first(list,expr(_,_i)) -> value (first)
 * all(list,expr(_,_i)) -> boolean
 * for(list,expr(_,_i),exit(_,_i)) -> success_count
 */
public class Expression implements Cloneable
{
    private static final Map<String, Integer> precedence = new HashMap<String,Integer>() {{
        put("unary+-!", 60);
        put("exponent^", 40);
        put("multiplication*/%", 30);
        put("addition+-", 20);
        put("compare>=><=<", 10);
        put("equal==!=", 7);
        put("and&&", 4);
        put("or||", 3);
        put("assign=<>", 2);
        put("nextop;", 1);
    }};
    private static final Random randomizer = new Random();

    public static final Value PI = new NumericValue(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");

    public static final Value euler = new NumericValue(
            "2.71828182845904523536028747135266249775724709369995957496696762772407663");

    /** The current infix expression */
    private String expression;
    public String getCodeString() {return expression;}

    private String name;
    public String getName() {return name;}

    /** Cached AST (Abstract Syntax Tree) (root) of the expression */
    private LazyValue ast = null;

    /** script specific operatos and built-in functions */
    private Map<String, ILazyOperator> operators = new HashMap<>();
    boolean isAnOperator(String opname) { return operators.containsKey(opname) || operators.containsKey(opname+"u");}

    private Map<String, ILazyFunction> functions = new HashMap<>();

    public static final Map<String, UserDefinedFunction> global_functions = new HashMap<>();

    public static final Map<String, LazyValue> global_variables = new HashMap<>();

    final Map<String, LazyValue> defaultVariables = new HashMap<>();

    /* should the evaluator output value of each ;'s statement during execution */
    private Consumer<String> logOutput = null;
    public Consumer<String> getLogger() {return logOutput;}
    void setLogOutput(Consumer<String> to) { logOutput = to; }

    @Override
    protected Expression clone() throws CloneNotSupportedException
    {
        // very very shallow copy for global functions to grab the context for error msgs
        Expression copy = (Expression) super.clone();
        copy.expression = this.expression;
        copy.name = this.name;
        return copy;
    }

    /* The expression evaluators exception class. */
    static class ExpressionException extends RuntimeException
    {
        private static TriFunction<Expression, Tokenizer.Token, String, List<String>> errorMaker = (expr, token, errmessage) ->
        {

            List<String> snippet = getExpressionSnippet(token, expr.expression);
            List<String> errMsg = new ArrayList<>(snippet);
            if (snippet.size() != 1)
            {
                errmessage+= " at line "+(token.lineno+1)+", pos "+(token.linepos+1);
            }
            else
            {
                errmessage += " at pos "+(token.pos+1);
            }
            if (expr.name != null)
            {
                errmessage += " ("+expr.name+")";
            }
            errMsg.add(errmessage);
            return errMsg;
        };
        public static TriFunction<Expression, Tokenizer.Token, String, List<String>> errorSnooper = null;

        ExpressionException(String message)
        {
            super(message);
        }
        public static String makeMessage(Expression e, Tokenizer.Token t, String message) throws ExpressionException
        {
            if (errorSnooper != null)
            {
                List<String> alternative = errorSnooper.apply(e, t, message);
                if (alternative!= null)
                {
                    return String.join("\n", alternative);
                }
            }
            return String.join("\n", errorMaker.apply(e, t, message));
        }

        ExpressionException(Expression e, Tokenizer.Token t, String message)
        {
            super(makeMessage(e, t, message));
        }
    }
    /* The internal expression evaluators exception class. */
    static class InternalExpressionException extends ExpressionException
    {
        InternalExpressionException(String message)
        {
            super(message);
        }
    }
    /* Exception thrown to terminate execution mid expression (aka return statement) */
    private static class ExitStatement extends RuntimeException
    {
        Value retval;
        ExitStatement(Value value)
        {
            retval = value;
        }
    }


    public static List<String> getExpressionSnippet(Tokenizer.Token token, String expr)
    {

        List<String> output = new ArrayList<>();
        for (String line: getExpressionSnippetLeftContext(token, expr, 1))
        {
            output.add(line);
        }
        List<String> context = getExpressionSnippetContext(token, expr);
        output.add(context.get(0)+" HERE>> "+context.get(1));
        for (String line: getExpressionSnippetRightContext(token, expr, 1))
        {
            output.add(line);
        }
        return output;
    }

    public static List<String> getExpressionSnippetLeftContext(Tokenizer.Token token, String expr, int contextsize)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length == 1) return output;
        for (int lno=token.lineno-1; lno >=0 && output.size() < contextsize; lno-- )
        {
            output.add(lines[lno]);
        }
        Collections.reverse(output);
        return output;
    }

    public static List<String> getExpressionSnippetContext(Tokenizer.Token token, String expr)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length > 1)
        {
            output.add(lines[token.lineno].substring(0, token.linepos));
            output.add(lines[token.lineno].substring(token.linepos));
        }
        else
        {
            output.add( expr.substring(max(0, token.pos-40), token.pos));
            output.add( expr.substring(token.pos, min(token.pos+1+40, expr.length())));
        }
        return output;
    }

    public static List<String> getExpressionSnippetRightContext(Tokenizer.Token token, String expr, int contextsize)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length == 1) { return output; }
        for (int lno=token.lineno+1; lno < lines.length && output.size() < contextsize; lno++ )
        {
            output.add(lines[lno]);
        }
        return output;
    }


    private static <T> T assertNotNull(T t)
    {
        if (t == null)
            throw new InternalExpressionException("Operand may not be null");
        return t;
    }

    private static <T> void assertNotNull(T t1, T t2)
    {
        if (t1 == null)
            throw new InternalExpressionException("First operand may not be null");
        if (t2 == null)
            throw new InternalExpressionException("Second operand may not be null");
    }

    static NumericValue getNumericValue(Value v1)
    {
        if (!(v1 instanceof NumericValue))
            throw new InternalExpressionException("Operand has to be of a numeric type");
        return ((NumericValue) v1);
    }

    private void addLazyUnaryOperator(String surface, int precedence, boolean leftAssoc,
                                       TriFunction<Context, Integer, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface+"u", new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer t, Expression e, Tokenizer.Token token, LazyValue v, LazyValue v2)
            {
                try
                {
                    if (v2 != null)
                    {
                        throw new ExpressionException(e, token, "Did not expect a second parameter for unary operator");
                    }
                    assertNotNull(v);
                    return lazyfun.apply(c, t, v);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, token, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(e, token, "Your math is wrong, "+exc.getMessage());
                }
            }
        });
    }


    private void addLazyBinaryOperatorWithDelegation(String surface, int precedence, boolean leftAssoc,
                                       SexFunction<Context, Integer, Expression, Tokenizer.Token, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2)
            {
                try
                {
                    assertNotNull(v1, v2);
                    return lazyfun.apply(c, type, e, t, v1, v2);
                }
                catch (InternalExpressionException exc) // might not actually throw it
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
                }
            }
        });
    }

    private void addLazyBinaryOperator(String surface, int precedence, boolean leftAssoc,
                                       QuadFunction<Context, Integer, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer t, Expression e, Tokenizer.Token token, LazyValue v1, LazyValue v2)
            {
                try
                {
                    assertNotNull(v1, v2);
                    return lazyfun.apply(c, t, v1, v2);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, token, exc.getMessage());
                }
                catch (ArithmeticException exc)
                {
                    throw new ExpressionException(e, token, "Your math is wrong, "+exc.getMessage());
                }
            }
        });
    }

    private void addUnaryOperator(String surface, boolean leftAssoc, Function<Value, Value> fun)
    {
        operators.put(surface+"u", new AbstractUnaryOperator(precedence.get("unary+-!"), leftAssoc)
        {
            @Override
            public Value evalUnary(Value v1)
            {
                return fun.apply(assertNotNull(v1));
            }
        });
    }

    private void addBinaryOperator(String surface, int precedence, boolean leftAssoc, BiFunction<Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractOperator(precedence, leftAssoc)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return fun.apply(v1, v2);
            }
        });
    }


    private void addUnaryFunction(String name, Function<Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name,  new AbstractFunction(1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return fun.apply(assertNotNull(parameters.get(0)));
            }
        });
    }

    void addBinaryFunction(String name, BiFunction<Value, Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractFunction(2)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                Value v1 = parameters.get(0);
                Value v2 = parameters.get(1);
                assertNotNull(v1, v2);
                return fun.apply(v1, v2);
            }
        });
    }

    private void addFunction(String name, Function<List<Value>, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractFunction(-1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                for (Value v: parameters)
                    assertNotNull(v);
                return fun.apply(parameters);
            }
        });
    }

    private void addMathematicalUnaryFunction(String name, Function<Double, Double> fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(fun.apply(getNumericValue(v).getDouble())));
    }

    private void addMathematicalBinaryFunction(String name, BiFunction<Double, Double, Double> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(fun.apply(getNumericValue(w).getDouble(), getNumericValue(v).getDouble())));
    }


    void addLazyFunction(String name, int num_params, TriFunction<Context, Integer, List<LazyValue>, LazyValue> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractLazyFunction(num_params)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer i, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
                //catch (ArithmeticException exc)
                //{
                //    throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
                //}
            }
        });
    }
    private void addContextFunction(String name, Expression expr, Tokenizer.Token token, List<String> arguments, LazyValue code)
    {
        name = name.toLowerCase(Locale.ROOT);
        if (functions.containsKey(name))
            throw new ExpressionException(expr, token, "Function "+name+" would mask a built-in function");
        Expression function_context;
        try
        {
            function_context = expr.clone();
            function_context.name = name;
        }
        catch (CloneNotSupportedException e)
        {
            throw new ExpressionException(expr, token, "Problems in allocating global function "+name);
        }

        global_functions.put(name, new UserDefinedFunction(arguments, function_context, token)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, List<LazyValue> lazyParams)
            {
                if (arguments.size() != lazyParams.size()) // something that might be subject to change in the future
                {
                    throw new ExpressionException(e, t,
                            "Incorrect number of arguments for function "+name+
                            ". Should be "+arguments.size()+", not "+lazyParams.size()
                    );
                }

                Map<String, LazyValue> context = new HashMap<>();
                //saving context and placing args
                for (int i=0; i<arguments.size(); i++)
                {
                    String arg = arguments.get(i);
                    context.put(arg, c.getVariable(arg));
                    Value val = lazyParams.get(i).evalValue(c);
                    c.setVariable(arg, (cc, tt) -> val);
                    //c.setVariable(arg, lazyParams.get(i) ) overflows stack
                }
                Value retVal = code.evalValue(c, type); // todo not sure if we need to propagete type / consider boolean context in defined functions - answer seems ye

                //restoring context
                for (Map.Entry<String, LazyValue> entry : context.entrySet())
                {
                    if (entry.getValue() == null)
                    {
                        c.delVariable(entry.getKey());
                    }
                    else
                    {
                        c.setVariable(entry.getKey(), entry.getValue());
                    }
                }
                return (cc, tt) -> retVal;
            }
        });
    }


    /**
     * Creates a new expression instance from an expression string
     *
     * @param expression The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *                   <code>"sin(y)&gt;0 &amp; max(z, 3)&gt;3"</code>
     */
    public Expression(String expression)
    {
        this.name = null;
        expression = expression.trim().replaceAll(";+$", "");
        this.expression = expression.replaceAll("\\$", "\n");
        defaultVariables.put("euler", (c, t) -> euler);
        defaultVariables.put("pi", (c, t) -> PI);
        defaultVariables.put("null", (c, t) -> Value.NULL);
        defaultVariables.put("true", (c, t) -> Value.TRUE);
        defaultVariables.put("false", (c, t) -> Value.FALSE);

        //special variables for second order functions so we don't need to check them all the time
        defaultVariables.put("_", (c, t) -> new NumericValue(0).boundTo("_"));
        defaultVariables.put("_i", (c, t) -> new NumericValue(0).boundTo("_i"));
        defaultVariables.put("_a", (c, t) -> new NumericValue(0).boundTo("_a"));



        // artificial construct to handle user defined functions and function definitions
        addLazyFunction(".",-1, (c, t, lv) -> { // adjust based on c
            String name = lv.get(lv.size()-1).evalValue(c).getString();
            //lv.remove(lv.size()-1); // aint gonna cut it
            if (t != Context.SIGNATURE) // just call the function
            {
                if (!global_functions.containsKey(name))
                {
                    throw new InternalExpressionException("Function "+name+" is not defined yet");
                }
                List<LazyValue> lvargs = new ArrayList<>(lv.size()-1);
                for (int i=0; i< lv.size()-1; i++)
                {
                    lvargs.add(lv.get(i));
                }
                UserDefinedFunction acf = global_functions.get(name);
                return (cc, tt) -> acf.lazyEval(c, t, acf.expression, acf.token, lvargs).evalValue(c); ///!!!! dono might need to store expr and token in statics? (e? t?)
            }

            // gimme signature
            List<String> args = new ArrayList<>(lv.size() - 1);
            for (int i = 0; i < lv.size() - 1; i++)
            {
                Value v = lv.get(i).evalValue(c);
                if (!v.isBound())
                {
                    throw new InternalExpressionException("Only variables can be used in function signature, not  " + v.getString());
                }
                args.add(v.boundVariable);
            }
            return (cc, tt) -> new FunctionSignatureValue(name, args);
        });

        addLazyBinaryOperator(";",precedence.get("nextop;"), true, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.VOID);
            if (c.getLogger() != null)
                c.getLogger().accept(v1.getString());
            return lv2;
        });

        addBinaryOperator("+", precedence.get("addition+-"), true, Value::add);
        addBinaryOperator("-", precedence.get("addition+-"), true, Value::subtract);
        addBinaryOperator("*", precedence.get("multiplication*/%"), true, Value::multiply);
        addBinaryOperator("/", precedence.get("multiplication*/%"), true, Value::divide);
        addBinaryOperator("%", precedence.get("multiplication*/%"), true, (v1, v2) ->
                new NumericValue(getNumericValue(v1).getDouble() % getNumericValue(v2).getDouble()));
        addBinaryOperator("^", precedence.get("exponent^"), false, (v1, v2) ->
                new NumericValue(Math.pow(getNumericValue(v1).getDouble(), getNumericValue(v2).getDouble())));

        addLazyBinaryOperator("&&", precedence.get("and&&"), false, (c, t, lv1, lv2) ->
        {
            boolean b1 = lv1.evalValue(c, Context.BOOLEAN).getBoolean();
            if (!b1) return LazyValue.FALSE;
            boolean b2 = lv2.evalValue(c, Context.BOOLEAN).getBoolean();
            return b2 ? LazyValue.TRUE : LazyValue.FALSE;
        });

        addLazyBinaryOperator("||", precedence.get("or||"), false, (c, t, lv1, lv2) ->
        {
            boolean b1 = lv1.evalValue(c, Context.BOOLEAN).getBoolean();
            if (b1) return LazyValue.TRUE;
            boolean b2 = lv2.evalValue(c, Context.BOOLEAN).getBoolean();
            return b2 ? LazyValue.TRUE : LazyValue.FALSE;
        });

        addBinaryOperator("~", precedence.get("compare>=><=<"), true, Value::in);

        addBinaryOperator(">", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) > 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator(">=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) >= 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("<", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) < 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("<=", precedence.get("compare>=><=<"), false, (v1, v2) ->
                v1.compareTo(v2) <= 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("==", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.TRUE : Value.FALSE);
        addBinaryOperator("!=", precedence.get("equal==!="), false, (v1, v2) ->
                v1.equals(v2) ? Value.FALSE : Value.TRUE);

        addLazyBinaryOperator("=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue && v2 instanceof ListValue)
            {
                 List<Value> ll = ((ListValue)v1).getItems();
                 List<Value> rl = ((ListValue)v2).getItems();
                 if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                 if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                 for (Value v: ll) v.assertAssignable();
                 Iterator<Value> li = ll.iterator();
                 Iterator<Value> ri = rl.iterator();
                 while(li.hasNext())
                 {
                     String lname = li.next().getVariable();
                     Value vval = ri.next();
                     c.setVariable(lname, (cc, tt) -> vval.boundTo(lname));
                 }
                 return (cc, tt) -> Value.TRUE;
            }
            v1.assertAssignable();
            String varname = v1.getVariable();
            LazyValue boundedLHS = (cc, tt) -> v2.boundTo(varname);
            c.setVariable(varname, boundedLHS);
            return boundedLHS;
        });

        addLazyBinaryOperator("+=", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            if (v1 instanceof ListValue && v2 instanceof ListValue)
            {
                List<Value> ll = ((ListValue)v1).getItems();
                List<Value> rl = ((ListValue)v2).getItems();
                if (ll.size() < rl.size()) throw new InternalExpressionException("Too many values to unpack");
                if (ll.size() > rl.size()) throw new InternalExpressionException("Too few values to unpack");
                for (Value v: ll) v.assertAssignable();
                Iterator<Value> li = ll.iterator();
                Iterator<Value> ri = rl.iterator();
                while(li.hasNext())
                {
                    Value lval = li.next();
                    String lname = lval.getVariable();
                    Value vval = ri.next();
                    c.setVariable(lname, (cc, tt) -> lval.add(vval).boundTo(lname));
                }
                return (cc, tt) -> Value.TRUE;
            }
            v1.assertAssignable();
            String varname = v1.getVariable();
            LazyValue boundedLHS = (cc, tt) -> v1.add(v2).boundTo(varname);
            c.setVariable(varname, boundedLHS);
            return boundedLHS;
        });

        //assigns const procedure to the lhs, returning its previous value
        addLazyBinaryOperatorWithDelegation("->", precedence.get("assign=<>"), false, (c, type, e, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.SIGNATURE);
            if (v1 instanceof FunctionSignatureValue)
            {
                FunctionSignatureValue sign = (FunctionSignatureValue) v1;
                addContextFunction(sign.getName(), e, t, sign.getArgs(), lv2);
            }
            else
            {
                v1.assertAssignable();
                c.setVariable(v1.getVariable(), lv2);
            }
            return (cc, tt) -> new StringValue("OK");
        });

        addLazyBinaryOperator("<>", precedence.get("assign=<>"), false, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c);
            Value v2 = lv2.evalValue(c);
            if (!v1.isBound() || !v2.isBound())
                throw new InternalExpressionException("Both sides of swapping assignment need to be variables");
            String lvalvar = v1.getVariable();
            String rvalvar = v2.getVariable();
            if (lvalvar.startsWith("_") || rvalvar.startsWith("_"))
                throw new InternalExpressionException("Cannot swap with local built-in variables, i.e. those that start with '_'");
            Value lval = v2.boundTo(lvalvar);
            Value rval = v1.boundTo(rvalvar);
            c.setVariable(lvalvar, (cc, tt) -> lval);
            c.setVariable(rvalvar, (cc, tt) -> rval);
            return (cc, tt) -> lval;
        });

        addUnaryOperator("-",  false, (v) -> new NumericValue(-getNumericValue(v).getDouble()));

        addUnaryOperator("+", false, (v) -> new NumericValue(getNumericValue(v).getDouble()));

        addLazyUnaryOperator("!", precedence.get("unary+-!"), false, (c, t, lv)-> lv.evalValue(c, Context.BOOLEAN).getBoolean() ? (cc, tt)-> Value.FALSE : (cc, tt) -> Value.TRUE); // might need context boolean

        addLazyFunction("not", 1, (c, t, lv) -> lv.get(0).evalValue(c, Context.BOOLEAN).getBoolean() ? ((cc, tt) -> Value.FALSE) : ((cc, tt) -> Value.TRUE));

        addUnaryFunction("fact", (v) ->
        {
            long number = getNumericValue(v).getLong();
            long factorial = 1;
            for (int i = 1; i <= number; i++)
            {
                factorial = factorial * i;
            }
            return new NumericValue(factorial);
        });


        addMathematicalUnaryFunction("sin",    (d) -> Math.sin(Math.toRadians(d)));
        addMathematicalUnaryFunction("cos",    (d) -> Math.cos(Math.toRadians(d)));
        addMathematicalUnaryFunction("tan",    (d) -> Math.tan(Math.toRadians(d)));
        addMathematicalUnaryFunction("asin",   (d) -> Math.toDegrees(Math.asin(d)));
        addMathematicalUnaryFunction("acos",   (d) -> Math.toDegrees(Math.acos(d)));
        addMathematicalUnaryFunction("atan",   (d) -> Math.toDegrees(Math.atan(d)));
        addMathematicalBinaryFunction("atan2", (d, d2) -> Math.toDegrees(Math.atan2(d, d2)) );
        addMathematicalUnaryFunction("sinh",   Math::sinh );
        addMathematicalUnaryFunction("cosh",   Math::cosh  );
        addMathematicalUnaryFunction("tanh",   Math::tanh );
        addMathematicalUnaryFunction("sec",    (d) ->  1.0 / Math.cos(Math.toRadians(d)) ); // Formula: sec(x) = 1 / cos(x)
        addMathematicalUnaryFunction("csc",    (d) ->  1.0 / Math.sin(Math.toRadians(d)) ); // Formula: csc(x) = 1 / sin(x)
        addMathematicalUnaryFunction("sech",   (d) ->  1.0 / Math.cosh(d) );                // Formula: sech(x) = 1 / cosh(x)
        addMathematicalUnaryFunction("csch",   (d) -> 1.0 / Math.sinh(d)  );                // Formula: csch(x) = 1 / sinh(x)
        addMathematicalUnaryFunction("cot",    (d) -> 1.0 / Math.tan(Math.toRadians(d))  ); // Formula: cot(x) = cos(x) / sin(x) = 1 / tan(x)
        addMathematicalUnaryFunction("acot",   (d) ->  Math.toDegrees(Math.atan(1.0 / d)) );// Formula: acot(x) = atan(1/x)
        addMathematicalUnaryFunction("coth",   (d) ->  1.0 / Math.tanh(d) );                // Formula: coth(x) = 1 / tanh(x)
        addMathematicalUnaryFunction("asinh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) + 1))));  // Formula: asinh(x) = ln(x + sqrt(x^2 + 1))
        addMathematicalUnaryFunction("acosh",  (d) ->  Math.log(d + (Math.sqrt(Math.pow(d, 2) - 1))));  // Formula: acosh(x) = ln(x + sqrt(x^2 - 1))
        addMathematicalUnaryFunction("atanh",  (d) ->                                       // Formula: atanh(x) = 0.5*ln((1 + x)/(1 - x))
        {
            if (Math.abs(d) > 1 || Math.abs(d) == 1)
                throw new InternalExpressionException("Number must be |x| < 1");
            return 0.5 * Math.log((1 + d) / (1 - d));
        });
        addMathematicalUnaryFunction("rad",  Math::toRadians);
        addMathematicalUnaryFunction("deg", Math::toDegrees);
        addMathematicalUnaryFunction("ln", Math::log);
        addMathematicalUnaryFunction("ln1p", Math::log1p);
        addMathematicalUnaryFunction("log10", Math::log10);
        addMathematicalUnaryFunction("log", a -> Math.log(a)/Math.log(2));
        addMathematicalUnaryFunction("log1p", x -> Math.log1p(x)/Math.log(2));

        addMathematicalUnaryFunction("sqrt", Math::sqrt);
        addMathematicalUnaryFunction("abs", Math::abs);
        addMathematicalUnaryFunction("round", (d) -> (double)Math.round(d));
        addMathematicalUnaryFunction("floor", Math::floor);
        addMathematicalUnaryFunction("ceil", Math::ceil);

        addLazyFunction("rand", 1, (c, t, lv) -> {
            Value argument = lv.get(0).evalValue(c);
            if (argument instanceof ListValue)
            {
                List<Value> list = ((ListValue) argument).getItems();
                return (cc, tt) -> list.get(randomizer.nextInt(list.size()));
            }
            if (t == Context.BOOLEAN)
            {
                double rv = getNumericValue(argument).getDouble()*randomizer.nextFloat();
                return (cc, tt) -> rv<1.0D?Value.FALSE:Value.TRUE;
            }

            return (cc, tt) -> new NumericValue(getNumericValue(argument).getDouble()*randomizer.nextFloat());
        });

        addLazyFunction("mandelbrot", 3, (c, t, lv) -> {
            double a0 = getNumericValue(lv.get(0).evalValue(c)).getDouble();
            double b0 = getNumericValue(lv.get(1).evalValue(c)).getDouble();
            long maxiter = getNumericValue(lv.get(2).evalValue(c)).getLong();
            double a = 0.0D;
            double b = 0.0D;
            long iter = 0;
            while(a*a+b*b<4 && iter < maxiter)
            {
                double temp = a*a-b*b+a0;
                b = 2*a*b+b0;
                a = temp;
                iter++;
            }
            long iFinal = iter;
            return (cc, tt) -> new NumericValue(iFinal);
        });

        addFunction("max", (lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("MAX requires at least one parameter");
            Value max = null;
            for (Value parameter : lv)
            {
                if (max == null || parameter.compareTo(max) > 0) max = parameter;
            }
            return max;
        });

        addFunction("min", (lv) ->
        {
            if (lv.size() == 0)
                throw new InternalExpressionException("MIN requires at least one parameter");
            Value min = null;
            for (Value parameter : lv)
            {
                if (min == null || parameter.compareTo(min) < 0) min = parameter;
            }
            return min;
        });

        addFunction("sort", (lv) ->
        {
            List<Value> toSort = lv;
            if (lv.size()==1 && lv.get(0) instanceof ListValue)
            {
                toSort = ((ListValue)lv.get(1)).getItems();
            }
            Collections.sort(toSort);
            return ListValue.wrap(toSort);
        });

        addUnaryFunction("relu", (v) -> v.compareTo(Value.ZERO) < 0 ? Value.ZERO : v);
        addUnaryFunction("print", (v) ->
        {
            System.out.println(v.getString());
            return v; // pass through for variables
        });
        addUnaryFunction("sleep", (v) ->
        {
            long time = getNumericValue(v).getLong();
            try
            {
                Thread.sleep(time);
                Thread.yield();
            }
            catch (InterruptedException ignored) { }
            return v; // pass through for variables
        });
        addLazyFunction("time", 0, (c, t, lv) ->
                (cc, tt) -> new NumericValue(1.0*System.nanoTime()/1000));


        addUnaryFunction("return", (v) -> { throw new ExitStatement(v); });

        addFunction("l", ListValue::new);

        addUnaryFunction("range", (v) -> LazyListValue.range(getNumericValue(v).getLong()));

        addBinaryFunction("element", (v1, v2) -> {
            if (!(v1.getClass().equals(ListValue.class)))
            {
                throw new InternalExpressionException("First argument of element should be a list");
            }
            List<Value> items = ((ListValue)v1).getItems();
            long index = getNumericValue(v2).getLong();
            int numitems = items.size();
            long range = abs(index)/numitems;
            index += (range+2)*numitems;
            index = index % numitems;
            return items.get((int)index);
        });

        addLazyFunction("var", 1, (c, t, lv) -> {
            String varname = lv.get(0).evalValue(c).getString();
            if (!c.isAVariable(varname))
                c.setVariable(varname, Value.ZERO);
            return c.getVariable(varname);
        });

        addLazyFunction("undef", 1, (c, t, lv) ->
        {
            String varname = lv.get(0).evalValue(c).getString();
            if (varname.startsWith("_"))
                throw new InternalExpressionException("Cannot replace local built-in variables, i.e. those that start with '_'");
            if (varname.endsWith("*"))
            {
                varname = varname.replaceAll("\\*+$", "");
                for (String key: global_functions.keySet())
                {
                    if (key.startsWith(varname)) global_functions.remove(key);
                }
                for (String key: global_variables.keySet())
                {
                    if (key.startsWith(varname)) global_variables.remove(key);
                }
                c.clearAll(varname);
            }
            else
            {
                global_functions.remove(varname);
                global_variables.remove(varname);
                c.delVariable(varname);
            }
            return (cc, tt) -> Value.NULL;
        });


        addLazyFunction("vars", 1, (c, t, lv) -> {
            String prefix = lv.get(0).evalValue(c).getString();
            List<Value> values = new ArrayList<>();
            if (prefix.startsWith("global"))
            {
                for (String k: global_variables.keySet())
                {
                    if (k.startsWith(prefix))
                        values.add(new StringValue(k));
                }
            }
            else
            {
                for (String k: c.getAllVariableNames())
                {
                    if (k.startsWith(prefix))
                        values.add(new StringValue(k));
                }
            }
            return (cc, tt) -> ListValue.wrap(values);
        });

        // if(cond1, expr1, cond2, expr2, ..., ?default) => value
        addLazyFunction("if", -1, (c, t, lv) ->
        {
            if ( lv.size() < 2 )
                throw new InternalExpressionException("if statement needs to have at least one condition and one case");
            for (int i=0; i<lv.size()-1; i+=2)
            {
                if (lv.get(i).evalValue(c, Context.BOOLEAN).getBoolean())
                {
                    int iFinal = i;
                    return (cc, tt) -> lv.get(iFinal+1).evalValue(c);
                }
            }
            if (lv.size()%2 == 1)
                return (cc, tt) -> lv.get(lv.size() - 1).evalValue(c);
            return (cc, tt) -> new NumericValue(0);
        });

        //condition and expression will get a bound 'i'
        //returns last successful expression or false
        // while(cond, limit, expr) => ??
        //replaced with for
        addLazyFunction("while", 3, (c, t, lv) ->
        {
            long limit = getNumericValue(lv.get(1).evalValue(c)).getLong();
            LazyValue condition = lv.get(0);
            LazyValue expr = lv.get(2);
            long i = 0;
            Value lastOne = Value.ZERO;
            //scoping
            LazyValue _val = c.getVariable("_");
            c.setVariable("_",(cc, tt) -> new NumericValue(0).boundTo("_"));
            while (i<limit && condition.evalValue(c).getBoolean() )
            {
                lastOne = expr.evalValue(c);
                i++;
                long seriously = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(seriously).boundTo("_"));
            }
            //revering scope
            c.setVariable("_", _val);
            Value lastValueNoKidding = lastOne;
            return (cc, tt) -> lastValueNoKidding;
        });

        // loop(Num, expr, exit_condition) => last_value
        // loop(list, expr,
        // expr receives bounded variable '_' indicating iteration
        addLazyFunction("loop", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for loop, should be 2 or 3, not "+lv.size());
            }
            long limit = getNumericValue(lv.get(0).evalValue(c)).getLong();
            Value lastOne = Value.NULL;
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);
            //scoping
            LazyValue _val = c.getVariable("_");
            for (long i=0; i < limit; i++)
            {
                long whyYouAsk = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(whyYouAsk).boundTo("_"));
                lastOne = expr.evalValue(c);
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            //revering scope
            c.setVariable("_", _val);
            Value trulyLastOne = lastOne;
            return (cc, tt) -> trulyLastOne;
        });



        // map(list or Num, expr) => list_results
        // receives bounded variable '_' with the expression
        addLazyFunction("map", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for map, should be 2 or 3, not "+lv.size());
            }

            Value rval= lv.get(0).evalValue(c);

            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of map function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int doYouReally = i;
                c.setVariable("_", (cc, tt) -> next.boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(doYouReally).boundTo("_i"));
                result.add(expr.evalValue(c));
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            ((ListValue) rval).fatality();
            LazyValue ret = (cc, tt) -> ListValue.wrap(result);
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return ret;
        });

        // grep(list or num, expr, exit_expr) => list
        // receives bounded variable '_' with the expression, and "_i" with index
        // produces list of values for which the expression is true
        addLazyFunction("filter", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for filter, should be 2 or 3, not "+lv.size());
            }

            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of filter function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(expr.evalValue(c).getBoolean())
                    result.add(next);
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            ((ListValue) rval).fatality();
            LazyValue ret = (cc, tt) -> ListValue.wrap(result);
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return ret;
        });

        // first(list, expr) => elem or null
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns first element on the list for which the expr is true
        addLazyFunction("first", 2, (c, t, lv) ->
        {

            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of 'first' function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            Value result = Value.NULL;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(expr.evalValue(c).getBoolean())
                {
                    result = next;
                    break;
                }
            }
            //revering scope
            ((ListValue) rval).fatality();
            Value whyWontYouTrustMeJava = result;
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return (cc, tt) -> whyWontYouTrustMeJava;
        });


        // all(list, expr) => boolean
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns true if expr is true for all items
        addLazyFunction("all", 2, (c, t, lv) ->
        {
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of 'all' function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            LazyValue result = LazyValue.TRUE;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(!expr.evalValue(c).getBoolean())
                {
                    result = LazyValue.FALSE;
                    break;
                }
            }
            //revering scope
            ((ListValue) rval).fatality();
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return result;
        });


        // similar to map, but returns total number of successes
        // for(list, expr, exit_expr) => success_count
        // can be substituted for first and all, but first is more efficient and all doesn't require knowing list size
        addLazyFunction("for", -1, (c, t, lv) ->
        {
            if (lv.size()<2 || lv.size()>3)
            {
                throw new InternalExpressionException("Incorrect number of attributes for 'for', should be 2 or 3, not "+lv.size());
            }
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("Second argument of 'for' function should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();
            LazyValue expr = lv.get(1);
            LazyValue cond = null;
            if(lv.size() > 2) cond = lv.get(2);

            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            int successCount = 0;
            for (int i=0; iterator.hasNext(); i++)
            {
                Value next = iterator.next();
                int seriously = i;
                c.setVariable("_", (cc, tt) -> next.boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(expr.evalValue(c).getBoolean())
                    successCount++;
                if (cond != null && cond.evalValue(c).getBoolean())
                    break;
            }
            //revering scope
            ((ListValue) rval).fatality();
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            long promiseWontChange = successCount;
            return (cc, tt) -> new NumericValue(promiseWontChange);
        });


        // reduce(list, expr, ?acc) => value
        // reduces values in the list with expression that gets accumulator
        // each iteration expr receives acc - accumulator, and '_' - current list value
        // returned value is substituted to the accumulator
        addLazyFunction("reduce", 3, (c, t, lv) ->
        {
            LazyValue expr = lv.get(1);

            Value acc = lv.get(2).evalValue(c);
            Value rval= lv.get(0).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("First argument of 'reduce' should be a list or iterator");
            Iterator<Value> iterator = ((ListValue) rval).iterator();

            if (!iterator.hasNext())
            {
                Value seriouslyWontChange = acc;
                return (cc, tt) -> seriouslyWontChange;
            }

            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _acc = c.getVariable("_a");

            while (iterator.hasNext())
            {
                Value v = iterator.next();
                Value promiseWontChangeYou = acc;
                c.setVariable("_a", (cc, tt) -> promiseWontChangeYou.boundTo("_a"));
                c.setVariable("_", (cc, tt) -> v.boundTo("_"));
                acc = expr.evalValue(c);
            }
            //reverting scope
            ((ListValue) rval).fatality();
            c.setVariable("_a", _acc);
            c.setVariable("_", _val);

            Value hopeItsEnoughPromise = acc;
            return (cc, tt) -> hopeItsEnoughPromise;
        });
    }


    /**
     * Implementation of the <i>Shunting Yard</i> algorithm to transform an
     * infix expression to a RPN expression.
     *
     * @param expression The input expression in infx.
     * @return A RPN representation of the expression, with each token as a list
     * member.
     */
    private List<Tokenizer.Token> shuntingYard(String expression)
    {
        List<Tokenizer.Token> outputQueue = new ArrayList<>();
        Stack<Tokenizer.Token> stack = new Stack<>();

        Tokenizer tokenizer = new Tokenizer(this, expression);

        Tokenizer.Token lastFunction = null;
        Tokenizer.Token previousToken = null;
        while (tokenizer.hasNext())
        {
            Tokenizer.Token token;
            try
            {
                token = tokenizer.next();
            }
            catch (StringIndexOutOfBoundsException e)
            {
                throw new ExpressionException("Script ended prematurely");
            }
            switch (token.type)
            {
                case STRINGPARAM:
                    //stack.push(token); // changed that so strings are treated like literals
                    //break;
                case LITERAL:
                case HEX_LITERAL:
                    if (previousToken != null && (
                            previousToken.type == Tokenizer.Token.TokenType.LITERAL ||
                                    previousToken.type == Tokenizer.Token.TokenType.HEX_LITERAL ||
                                    previousToken.type == Tokenizer.Token.TokenType.STRINGPARAM))
                    {
                        throw new ExpressionException(this, token, "Missing operator");
                    }
                    outputQueue.add(token);
                    break;
                case VARIABLE:
                    outputQueue.add(token);
                    break;
                case FUNCTION:
                    stack.push(token);
                    lastFunction = token;
                    break;
                case COMMA:
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(this, previousToken, "Missing parameter(s) for operator ");
                    }
                    while (!stack.isEmpty() && stack.peek().type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        if (lastFunction == null)
                        {
                            throw new ExpressionException(this, token, "Unexpected comma");
                        }
                        else
                        {
                            throw new ExpressionException(this, lastFunction, "Parse error for function");
                        }
                    }
                    break;
                case OPERATOR:
                {
                    if (previousToken != null
                            && (previousToken.type == Tokenizer.Token.TokenType.COMMA || previousToken.type == Tokenizer.Token.TokenType.OPEN_PAREN))
                    {
                        throw new ExpressionException(this, token, "Missing parameter(s) for operator '" + token+"'");
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(this, token, "Unknown operator '" + token + "'");
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case UNARY_OPERATOR:
                {
                    if (previousToken != null && previousToken.type != Tokenizer.Token.TokenType.OPERATOR
                            && previousToken.type != Tokenizer.Token.TokenType.COMMA && previousToken.type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        throw new ExpressionException(this, token, "Invalid position for unary operator " + token );
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(this, token, "Unknown unary operator '" + token.surface.substring(0, token.surface.length() - 1) + "'");
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case OPEN_PAREN:
                    if (previousToken != null)
                    {
                        if (previousToken.type == Tokenizer.Token.TokenType.LITERAL || previousToken.type == Tokenizer.Token.TokenType.CLOSE_PAREN
                                || previousToken.type == Tokenizer.Token.TokenType.VARIABLE
                                || previousToken.type == Tokenizer.Token.TokenType.HEX_LITERAL)
                        {
                            // Implicit multiplication, e.g. 23(a+b) or (a+b)(a-b)
                            Tokenizer.Token multiplication = new Tokenizer.Token();
                            multiplication.append("*");
                            multiplication.type = Tokenizer.Token.TokenType.OPERATOR;
                            stack.push(multiplication);
                        }
                        // if the ( is preceded by a valid function, then it
                        // denotes the start of a parameter list
                        if (previousToken.type == Tokenizer.Token.TokenType.FUNCTION)
                        {
                            outputQueue.add(token);
                        }
                    }
                    stack.push(token);
                    break;
                case CLOSE_PAREN:
                    if (previousToken != null && previousToken.type == Tokenizer.Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(this, previousToken, "Missing parameter(s) for operator " + previousToken);
                    }
                    while (!stack.isEmpty() && stack.peek().type != Tokenizer.Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        throw new ExpressionException("Mismatched parentheses");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && stack.peek().type == Tokenizer.Token.TokenType.FUNCTION)
                    {
                        outputQueue.add(stack.pop());
                    }
            }
            previousToken = token;
        }

        while (!stack.isEmpty())
        {
            Tokenizer.Token element = stack.pop();
            if (element.type == Tokenizer.Token.TokenType.OPEN_PAREN || element.type == Tokenizer.Token.TokenType.CLOSE_PAREN)
            {
                throw new ExpressionException(this, element, "Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private void shuntOperators(List<Tokenizer.Token> outputQueue, Stack<Tokenizer.Token> stack, ILazyOperator o1)
    {
        Tokenizer.Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null
                && (nextToken.type == Tokenizer.Token.TokenType.OPERATOR
                || nextToken.type == Tokenizer.Token.TokenType.UNARY_OPERATOR)
                && ((o1.isLeftAssoc() && o1.getPrecedence() <= operators.get(nextToken.surface).getPrecedence())
                || (o1.getPrecedence() < operators.get(nextToken.surface).getPrecedence())))
        {
            outputQueue.add(stack.pop());
            nextToken = stack.isEmpty() ? null : stack.peek();
        }
    }




    /**
     * Evaluates the expression.
     *
     * @return The result of the expression.
     * @param c - Context with pre-initialized variables
     */
    public Value eval(Context c)
    {
        return eval(c, Context.NONE);
    }
    public Value eval(Context c, Integer expectedType)
    {
        if (ast == null)
        {
            ast = getAST();
        }
        try
        {
            return ast.evalValue(c, expectedType);
        }
        catch (ExitStatement exit)
        {
            return exit.retval;
        }
        catch (StackOverflowError ignored)
        {
            throw new ExpressionException("Your thoughts are too deep");
        }
    }

    private LazyValue getAST()
    {
        Stack<LazyValue> stack = new Stack<>();
        List<Tokenizer.Token> rpn = shuntingYard(this.expression);
        validate(rpn);
        for (final Tokenizer.Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                {
                    final LazyValue value = stack.pop();
                    LazyValue result = (c, t) -> operators.get(token.surface).lazyEval(c, t, this, token, value, null).evalValue(c);
                    stack.push(result);
                    break;
                }
                case OPERATOR:
                    final LazyValue v1 = stack.pop();
                    final LazyValue v2 = stack.pop();
                    LazyValue result = (c,t) -> operators.get(token.surface).lazyEval(c, t,this, token, v2, v1).evalValue(c);
                    stack.push(result);
                    break;
                case VARIABLE:
                    stack.push((c, t) ->
                    {
                        if (!c.isAVariable(token.surface)) // new variable
                        {
                            c.setVariable(token.surface, Value.ZERO);
                        }
                        LazyValue lazyVariable = c.getVariable(token.surface);
                        return lazyVariable.evalValue(c);
                    });
                    break;
                case FUNCTION:
                    String name = token.surface.toLowerCase(Locale.ROOT);
                    ILazyFunction f;
                    ArrayList<LazyValue> p;
                    boolean isKnown = functions.containsKey(name); // globals will be evaluated lazily, not at compile time via .
                    if (isKnown)
                    {
                        f = functions.get(name);
                        p = new ArrayList<>(!f.numParamsVaries() ? f.getNumParams() : 0);
                    }
                    else // potentially unknown function or just unknown function
                    {
                        f = functions.get(".");
                        p = new ArrayList<>();
                    }
                    // pop parameters off the stack until we hit the start of
                    // this function's parameter list
                    while (!stack.isEmpty() && stack.peek() != LazyValue.PARAMS_START)
                    {
                        p.add(0, stack.pop());
                    }
                    if (!isKnown) p.add( (c, t) -> new StringValue(name));

                    if (stack.peek() == LazyValue.PARAMS_START)
                    {
                        stack.pop();
                    }

                    stack.push((c, t) -> f.lazyEval(c, t, this, token, p).evalValue(c));
                    break;
                case OPEN_PAREN:
                    stack.push(LazyValue.PARAMS_START);
                    break;
                case LITERAL:
                    stack.push((c, t) ->
                    {
                        if (token.surface.equalsIgnoreCase("NULL"))
                            return Value.NULL;
                        return new NumericValue(token.surface);
                    });
                    break;
                case STRINGPARAM:
                    stack.push((c, t) -> new StringValue(token.surface) ); // was originally null
                    break;
                case HEX_LITERAL:
                    stack.push((c, t) -> new NumericValue(new BigInteger(token.surface.substring(2), 16).doubleValue()));
                    break;
                default:
                    throw new ExpressionException(this, token, "Unexpected token '" + token.surface + "'");
            }
        }
        return stack.pop();
    }

    /**
     * Check that the expression has enough numbers and variables to fit the
     * requirements of the operators and functions, also check for only 1 result
     * stored at the end of the evaluation.
     */
    private void validate(List<Tokenizer.Token> rpn)
    {
        /*-
         * Thanks to Norman Ramsey:
         * http://http://stackoverflow.com/questions/789847/postfix-notation-validation
         */
        // each push on to this stack is a new function scope, with the value of
        // each
        // layer on the stack being the count of the number of parameters in
        // that scope
        Stack<Integer> stack = new Stack<>();

        // push the 'global' scope
        stack.push(0);

        for (final Tokenizer.Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                    if (stack.peek() < 1)
                    {
                        throw new ExpressionException(this, token, "Missing parameter(s) for operator " + token);
                    }
                    break;
                case OPERATOR:
                    if (stack.peek() < 2)
                    {
                        if (token.surface.equalsIgnoreCase(";"))
                        {
                            throw new ExpressionException(this, token, "Unnecessary semicolon");
                        }
                        throw new ExpressionException(this, token, "Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.peek() - 2 + 1);
                    break;
                case FUNCTION:
                    ILazyFunction f = functions.get(token.surface.toLowerCase(Locale.ROOT));// don't validate global - userdef functions
                    int numParams = stack.pop();
                    if (f != null && !f.numParamsVaries() && numParams != f.getNumParams())
                    {
                        throw new ExpressionException(this, token, "Function " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    }
                    if (stack.size() <= 0)
                    {
                        throw new ExpressionException(this, token, "Too many function calls, maximum scope exceeded");
                    }
                    // push the result of the function
                    stack.set(stack.size() - 1, stack.peek() + 1);
                    break;
                case OPEN_PAREN:
                    stack.push(0);
                    break;
                default:
                    stack.set(stack.size() - 1, stack.peek() + 1);
            }
        }

        if (stack.size() > 1)
        {
            throw new ExpressionException("Too many unhandled function parameter lists");
        }
        else if (stack.peek() > 1)
        {
            throw new ExpressionException("Too many numbers or variables");
        }
        else if (stack.peek() < 1)
        {
            throw new ExpressionException("Empty expression");
        }
    }




    @FunctionalInterface
    public interface TriFunction<A, B, C, R> { R apply(A a, B b, C c); }
    @FunctionalInterface
    public interface QuadFunction<A, B, C, D, R> { R apply(A a, B b, C c, D d);}
    @FunctionalInterface
    public interface QuinnFunction<A, B, C, D, E, R> { R apply(A a, B b, C c, D d, E e);}
    @FunctionalInterface
    public interface SexFunction<A, B, C, D, E, F, R> { R apply(A a, B b, C c, D d, E e, F f);}

    private interface ILazyFunction
    {
        int getNumParams();

        boolean numParamsVaries();

        LazyValue lazyEval(Context c, Integer type, Expression expr, Tokenizer.Token token, List<LazyValue> lazyParams);
        // lazy function has a chance to change execution based on contxt
    }

    private interface IFunction extends ILazyFunction
    {
        Value eval(List<Value> parameters);
    }

    private interface ILazyOperator
    {
        int getPrecedence();

        boolean isLeftAssoc();

        LazyValue lazyEval(Context c, Integer type, Expression e, Tokenizer.Token t, LazyValue v1, LazyValue v2);
    }

    private interface IOperator extends ILazyOperator
    {
        Value eval(Value v1, Value v2);
    }

    public abstract static class UserDefinedFunction extends AbstractLazyFunction implements ILazyFunction
    {
        protected List<String> arguments;
        protected Expression expression;
        protected Tokenizer.Token token;
        UserDefinedFunction(List<String> args, Expression expr, Tokenizer.Token t)
        {
            super(args.size());
            arguments = args;
            expression = expr;
            token = t;
        }
        public List<String> getArguments()
        {
            return arguments;
        }
        public Expression getExpression()
        {
            return expression;
        }
        public Tokenizer.Token getToken()
        {
            return token;
        }



    }

    private abstract static class AbstractLazyFunction implements ILazyFunction
    {
        protected String name;
        int numParams;

        AbstractLazyFunction(int numParams)
        {
            this.numParams = numParams;
        }


        public String getName() {
            return name;
        }

        public int getNumParams() {
            return numParams;
        }

        public boolean numParamsVaries() {
            return numParams < 0;
        }
    }

    private abstract static class AbstractFunction extends AbstractLazyFunction implements IFunction
    {
        AbstractFunction(int numParams) {
            super(numParams);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Tokenizer.Token t, final List<LazyValue> lazyParams)
        {
            try
            {
                return new LazyValue()
                { // eager evaluation always ignores the required type and evals params by none default
                    private List<Value> params;

                    public Value evalValue(Context c, Integer type) {
                        return AbstractFunction.this.eval(getParams(c));
                    }

                    private List<Value> getParams(Context c) {
                        if (params == null) {
                            params = new ArrayList<>();
                            for (LazyValue lazyParam : lazyParams) {
                                params.add(lazyParam.evalValue(c)); // none type default by design
                            }
                        }
                        return params;
                    }
                };
            }
            catch (InternalExpressionException exc)
            {
                throw new ExpressionException(e, t, exc.getMessage());
            }
        }
    }

    private abstract static class AbstractLazyOperator implements ILazyOperator
    {
        int precedence;

        boolean leftAssoc;

        AbstractLazyOperator(int precedence, boolean leftAssoc) {
            this.precedence = precedence;
            this.leftAssoc = leftAssoc;
        }

        public int getPrecedence() {
            return precedence;
        }

        public boolean isLeftAssoc() {
            return leftAssoc;
        }

    }

    private abstract static class AbstractOperator extends AbstractLazyOperator implements IOperator
    {

        AbstractOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }

        @Override
        public LazyValue lazyEval(Context c_ignored, Integer type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            try
            {
                return (c, type_ignored) -> AbstractOperator.this.eval(v1.evalValue(c), v2.evalValue(c));
            }
            catch (InternalExpressionException exc)
            {
                throw new ExpressionException(e, t, exc.getMessage());
            }
            catch (ArithmeticException exc)
            {
                throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
            }
        }
    }

    private abstract static class AbstractUnaryOperator extends AbstractOperator
    {
        AbstractUnaryOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Tokenizer.Token t, final LazyValue v1, final LazyValue v2)
        {
            try
            {
                if (v2 != null)
                {
                    throw new ExpressionException(e, t, "Did not expect a second parameter for unary operator");
                }
                return (c, ignored_type) -> AbstractUnaryOperator.this.evalUnary(v1.evalValue(c));
            }
            catch (InternalExpressionException exc) // might not actually throw it
            {
                throw new ExpressionException(e, t, exc.getMessage());
            }
            catch (ArithmeticException exc)
            {
                throw new ExpressionException(e, t, "Your math is wrong, "+exc.getMessage());
            }
        }

        @Override
        public Value eval(Value v1, Value v2)
        {
            throw new ExpressionException("Shouldn't end up here");
        }

        public abstract Value evalUnary(Value v1);
    }
}
