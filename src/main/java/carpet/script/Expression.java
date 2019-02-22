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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

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

    /** The {@link MathContext} to use for calculations. */
    private MathContext mc;

    /** The current infix expression */
    private String expression;
    public String getCodeString() {return expression;}

    private String name;
    public String getName() {return name;}

    /** The cached RPN (Reverse Polish Notation) of the expression. */
    private List<Token> rpn = null;

    /** Cached AST (Abstract Syntax Tree) (root) of the expression */
    private LazyValue ast = null;

    private Map<String, ILazyOperator> operators = new HashMap<>();

    private Map<String, ILazyFunction> functions = new HashMap<>();

    public static Map<String, AbstractContextFunction> global_functions = new HashMap<>();

    public static Map<String, LazyValue> global_variables = new HashMap<>();



    Map<String, LazyValue> defaultVariables = new HashMap<>();

    /** should the evaluator output value of each ;'s statement during execution */
    private Consumer<String> logOutput = null;


    @Override
    protected Expression clone() throws CloneNotSupportedException
    {
        // very very shallow copy for global functions to grab the context for error msgs
        Expression copy = (Expression) super.clone();
        copy.expression = this.expression;
        copy.name = this.name;
        return copy;
    }
    /** LazyNumber interface created for lazily evaluated functions */
    @FunctionalInterface
    public interface LazyValue
    {
        LazyValue FALSE = (c, t) -> Value.FALSE;
        LazyValue TRUE = (c, t) -> Value.TRUE;
        LazyValue NULL = (c, t) -> Value.NULL;
        LazyValue ZERO = (c, t) -> Value.ZERO;
        /**
         * The Value representation of the left parenthesis, used for parsing
         * varying numbers of function parameters.
         */
        LazyValue PARAMS_START = (c, t) -> null;

        Value evalValue(Context c, Integer type);

        default Value evalValue(Context c){
            return evalValue(c, 0);
        }
    }

    /** The expression evaluators exception class. */
    static class ExpressionException extends RuntimeException
    {

        private static TriFunction<Expression,Token, String, List<String>> errorMaker = (expr, token, errmessage) ->
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
        public static TriFunction<Expression,Token, String, List<String>> errorSnooper = null;

        ExpressionException(String message)
        {
            super(message);
        }
        public static String makeMessage(Expression e, Token t, String message) throws ExpressionException
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

        ExpressionException(Expression e, Token t, String message)
        {
            super(makeMessage(e, t, message));
        }
    }
    /** The internal expression evaluators exception class. */
    static class InternalExpressionException extends ExpressionException
    {
        InternalExpressionException(String message)
        {
            super(message);
        }
    }
    /** Exception thrown to terminate execution mid expression (aka return statement) */
    private static class ExitStatement extends RuntimeException
    {
        Value retval;
        ExitStatement(Value value)
        {
            retval = value;
        }
    }
    public static List<String> getExpressionSnippet(Token token, String expr)
    {
        List<String> output = new ArrayList<>();
        String[] lines = expr.split("\n");
        if (lines.length > 1)
        {
            if (token.lineno > 0)
            {
                output.add(lines[token.lineno-1]);
            }
            output.add(lines[token.lineno].substring(0, token.linepos)+" HERE>> "+
                    lines[token.lineno].substring(token.linepos));

            if (token.lineno < lines.length-1)
            {
                output.add(lines[token.lineno+1]);
            }
            return output;
        }
        else
        {
            output.add(
            expr.substring(max(0, token.pos-40), token.pos)+" HERE>> "+
                    expr.substring(token.pos, min(token.pos+1+40, expr.length())));
        }
        return output;
    }


    public static class Token
    {
        enum TokenType
        {
            VARIABLE, FUNCTION, LITERAL, OPERATOR, UNARY_OPERATOR,
            OPEN_PAREN, COMMA, CLOSE_PAREN, HEX_LITERAL, STRINGPARAM
        }
        public String surface = "";
        public TokenType type;
        public int pos;
        public int linepos;
        public int lineno;

        public void append(char c)
        {
            surface += c;
        }

        public void append(String s)
        {
            surface += s;
        }

        public char charAt(int pos)
        {
            return surface.charAt(pos);
        }

        public int length()
        {
            return surface.length();
        }

        @Override
        public String toString()
        {
            return surface;
        }
    }

    /**
     * Expression tokenizer that allows to iterate over a {@link String}
     * expression token by token. Blank characters will be skipped.
     */
    private static class Tokenizer implements Iterator<Token>
    {

        /** What character to use for decimal separators. */
        private static final char decimalSeparator = '.';
        /** What character to use for minus sign (negative values). */
        private static final char minusSign = '-';
        /** Actual position in expression string. */
        private int pos = 0;
        private int lineno = 0;
        private int linepos = 0;


        /** The original input expression. */
        private String input;
        /** The previous token or <code>null</code> if none. */
        private Token previousToken;

        private Expression expression;

        Tokenizer(Expression expr, String input)
        {
            this.input = input;
            this.expression = expr;
        }

        @Override
        public boolean hasNext()
        {
            return (pos < input.length());
        }

        /**
         * Peek at the next character, without advancing the iterator.
         *
         * @return The next character or character 0, if at end of string.
         */
        private char peekNextChar()
        {
            return (pos < (input.length() - 1)) ? input.charAt(pos + 1) : 0;
        }

        private boolean isHexDigit(char ch)
        {
            return ch == 'x' || ch == 'X' || (ch >= '0' && ch <= '9') || (ch >= 'a' && ch <= 'f')
                    || (ch >= 'A' && ch <= 'F');
        }

        @Override
        public Token next()
        {
            Token token = new Token();

            if (pos >= input.length())
            {
                return previousToken = null;
            }
            char ch = input.charAt(pos);
            while (Character.isWhitespace(ch) && pos < input.length())
            {
                linepos++;
                if (ch=='\n')
                {
                    lineno++;
                    linepos = 0;
                }
                ch = input.charAt(++pos);
            }
            token.pos = pos;
            token.lineno = lineno;
            token.linepos = linepos;

            boolean isHex = false;

            if (Character.isDigit(ch) || (ch == decimalSeparator && Character.isDigit(peekNextChar())))
            {
                if (ch == '0' && (peekNextChar() == 'x' || peekNextChar() == 'X'))
                    isHex = true;
                while ((isHex
                        && isHexDigit(
                        ch))
                        || (Character.isDigit(ch) || ch == decimalSeparator || ch == 'e' || ch == 'E'
                        || (ch == minusSign && token.length() > 0
                        && ('e' == token.charAt(token.length() - 1)
                        || 'E' == token.charAt(token.length() - 1)))
                        || (ch == '+' && token.length() > 0
                        && ('e' == token.charAt(token.length() - 1)
                        || 'E' == token.charAt(token.length() - 1))))
                        && (pos < input.length()))
                {
                    token.append(input.charAt(pos++));
                    linepos++;
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                token.type = isHex ? Token.TokenType.HEX_LITERAL : Token.TokenType.LITERAL;
            }
            else if (ch == '\'')
            {
                pos++;
                linepos++;
                if (previousToken == null || previousToken.type != Token.TokenType.STRINGPARAM)
                {
                    ch = input.charAt(pos);
                    while (ch != '\'')
                    {
                        token.append(input.charAt(pos++));
                        linepos++;
                        ch = pos == input.length() ? 0 : input.charAt(pos);
                    }
                    pos++;
                    linepos++;
                    token.type = Token.TokenType.STRINGPARAM;
                }
                else
                {
                    return next();
                }
            }
            else if (Character.isLetter(ch) || "_".indexOf(ch) >= 0)
            {
                while ((Character.isLetter(ch) || Character.isDigit(ch) || "_".indexOf(ch) >= 0
                        || token.length() == 0 && "_".indexOf(ch) >= 0) && (pos < input.length()))
                {
                    token.append(input.charAt(pos++));
                    linepos++;
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                // Remove optional white spaces after function or variable name
                if (Character.isWhitespace(ch))
                {
                    while (Character.isWhitespace(ch) && pos < input.length())
                    {
                        ch = input.charAt(pos++);
                        linepos++;
                        if (ch=='\n')
                        {
                            lineno++;
                            linepos = 0;
                        }
                    }
                    pos--;
                    linepos--;
                }
                token.type = ch == '(' ? Token.TokenType.FUNCTION : Token.TokenType.VARIABLE;
            }
            else if (ch == '(' || ch == ')' || ch == ',')
            {
                if (ch == '(')
                {
                    token.type = Token.TokenType.OPEN_PAREN;
                }
                else if (ch == ')')
                {
                    token.type = Token.TokenType.CLOSE_PAREN;
                }
                else
                {
                    token.type = Token.TokenType.COMMA;
                }
                token.append(ch);
                pos++;
                linepos++;
            }
            else
            {
                String greedyMatch = "";
                int initialPos = pos;
                ch = input.charAt(pos);
                int validOperatorSeenUntil = -1;
                while (!Character.isLetter(ch) && !Character.isDigit(ch) && "_".indexOf(ch) < 0
                        && !Character.isWhitespace(ch) && ch != '(' && ch != ')' && ch != ','
                        && (pos < input.length()))
                {
                    greedyMatch += ch;
                    pos++;
                    linepos++;
                    if (this.expression.operators.containsKey(greedyMatch))
                    {
                        validOperatorSeenUntil = pos;
                    }
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                if (validOperatorSeenUntil != -1)
                {
                    token.append(input.substring(initialPos, validOperatorSeenUntil));
                    pos = validOperatorSeenUntil;
                }
                else
                {
                    token.append(greedyMatch);
                }

                if (previousToken == null || previousToken.type == Token.TokenType.OPERATOR
                        || previousToken.type == Token.TokenType.OPEN_PAREN || previousToken.type == Token.TokenType.COMMA)
                {
                    token.surface += "u";
                    token.type = Token.TokenType.UNARY_OPERATOR;
                }
                else
                {
                    token.type = Token.TokenType.OPERATOR;
                }
            }
            return previousToken = token;
        }

        @Override
        public void remove()
        {
            throw new InternalExpressionException("remove() not supported");
        }

    }

    private static Value assertNotNull(Value v1)
    {
        if (v1 == null)
            throw new InternalExpressionException("Operand may not be null");
        return v1;
    }

    static BigDecimal getNumericalValue(Value v1)
    {
        if (!(v1 instanceof NumericValue))
            throw new InternalExpressionException("Operand has to be of a numeric type");
        return ((NumericValue) v1).getNumber();
    }

    private static void assertNotNull(Value v1, Value v2)
    {
        if (v1 == null)
            throw new InternalExpressionException("First operand may not be null");
        if (v2 == null)
            throw new InternalExpressionException("Second operand may not be null");
    }

    private static void assertNotNull(LazyValue lv, LazyValue lv2)
    {
        if (lv == null)
            throw new InternalExpressionException("Operand may not be null");
        if (lv2 == null)
            throw new InternalExpressionException("Operand may not be null");
    }


    private void addLazyBinaryOperatorWithDelegation(String surface, int precedence, boolean leftAssoc,
                                       SexFunction<Context, Integer, Expression, Token, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Token t, LazyValue v1, LazyValue v2)
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
            }
        });
    }

    private void addLazyBinaryOperator(String surface, int precedence, boolean leftAssoc,
                                       QuadFunction<Context, Integer, LazyValue, LazyValue, LazyValue> lazyfun)
    {
        operators.put(surface, new AbstractLazyOperator(precedence, leftAssoc)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer t, Expression e, Token token, LazyValue v1, LazyValue v2)
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
            }
        });
    }


    private void addUnaryOperator(String surface, boolean leftAssoc, Function<Value, Value> fun)
    {
        operators.put(surface+"u", new AbstractUnaryOperator(precedence.get("unary+-!"), leftAssoc)
        {
            @Override
            public Value evalUnary(Expression e, Token t, Value v1)
            {
                try
                {
                    return fun.apply(assertNotNull(v1));
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
            }
        });
    }

    private void addBinaryOperator(String surface, int precedence, boolean leftAssoc, BiFunction<Value, Value, Value> fun)
    {
        operators.put(surface, new AbstractOperator(precedence, leftAssoc)
        {
            @Override
            public Value eval(Expression e, Token t, Value v1, Value v2)
            {
                try
                {
                    assertNotNull(v1, v2);
                    return fun.apply(v1, v2);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
            }
        });
    }


    private void addUnaryFunction(String name, Function<Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name,  new AbstractFunction(1)
        {
            @Override
            public Value eval(Expression e, Token t, List<Value> parameters)
            {
                try
                {
                    return fun.apply(assertNotNull(parameters.get(0)));
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
            }
        });
    }

    void addBinaryFunction(String name, BiFunction<Value, Value, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractFunction(2)
        {
            @Override
            public Value eval(Expression e, Token t, List<Value> parameters)
            {
                try
                {
                    Value v1 = parameters.get(0);
                    Value v2 = parameters.get(1);
                    assertNotNull(v1, v2);
                    return fun.apply(v1, v2);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
            }
        });
    }

    private void addFunction(String name, Function<List<Value>, Value> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractFunction(-1)
        {
            @Override
            public Value eval(Expression e, Token t, List<Value> parameters)
            {
                try
                {
                    for (Value v: parameters)
                        assertNotNull(v);
                    return fun.apply(parameters);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
            }
        });
    }

    private void addMathematicalUnaryFunction(String name, Function<Double, Double> fun)
    {
        addUnaryFunction(name, (v) -> new NumericValue(new BigDecimal(fun.apply(getNumericalValue(v).doubleValue()),mc)));
    }

    private void addMathematicalBinaryFunction(String name, BiFunction<Double, Double, Double> fun)
    {
        addBinaryFunction(name, (w, v) ->
                new NumericValue(new BigDecimal(fun.apply(getNumericalValue(w).doubleValue(), getNumericalValue(v).doubleValue()), mc)));
    }


    void addLazyFunction(String name, int num_params, TriFunction<Context, Integer, List<LazyValue>, LazyValue> fun)
    {
        name = name.toLowerCase(Locale.ROOT);
        functions.put(name, new AbstractLazyFunction(num_params)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer i, Expression e, Token t, List<LazyValue> lazyParams)
            {
                try
                {
                    return fun.apply(c, i, lazyParams);
                }
                catch (InternalExpressionException exc)
                {
                    throw new ExpressionException(e, t, exc.getMessage());
                }
            }
        });
    }
    private void addContextFunction(String name, Expression expr, Token token, List<String> arguments, LazyValue code)
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

        global_functions.put(name, new AbstractContextFunction(arguments, function_context, token)
        {
            @Override
            public LazyValue lazyEval(Context c, Integer type, Expression e, Token t, List<LazyValue> lazyParams)
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
     * Creates a new expression instance from an expression string with a given
     * default match context of {@link MathContext#DECIMAL64}.
     *
     * @param expression The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *                   <code>"sin(y)>0 & max(z, 3)>3"</code>
     */
    public Expression(String expression)
    {
        this(expression, MathContext.DECIMAL64);
    }

    /**
     * Creates a new expression instance from an expression string with a given
     * default match context.
     *
     * @param expression         The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *                           <code>"sin(y)>0 & max(z, 3)>3"</code>
     * @param defaultMathContext The {@link MathContext} to use by default.
     */
    public Expression(String expression, MathContext defaultMathContext)
    {
        this.mc = defaultMathContext;
        this.name = null;
        expression = expression.trim().replaceAll(";+$", "");
        this.expression = expression.replaceAll("\\$", "\n");
        defaultVariables.put("e", (c, t) -> euler);
        defaultVariables.put("PI", (c, t) -> PI);
        defaultVariables.put("NULL", (c, t) -> Value.NULL);
        defaultVariables.put("TRUE", (c, t) -> Value.TRUE);
        defaultVariables.put("FALSE", (c, t) -> Value.FALSE);

        //special variables for second order functions so we don't need to check them all the time
        defaultVariables.put("_", (c, t) -> new NumericValue(0).boundTo("_"));
        defaultVariables.put("_i", (c, t) -> new NumericValue(0).boundTo("_i"));
        defaultVariables.put("_a", (c, t) -> new NumericValue(0).boundTo("_a"));

        addLazyBinaryOperator(";",precedence.get("nextop;"), true, (c, t, lv1, lv2) ->
        {
            Value v1 = lv1.evalValue(c, Context.VOID);//.withExpected(Context.VOID));
            if (c.logOutput != null)
                c.logOutput.accept(v1.getString());
            return lv2;
        });

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
                AbstractContextFunction acf = global_functions.get(name);
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

        addBinaryOperator("+", precedence.get("addition+-"), true, Value::add);
        addBinaryOperator("-", precedence.get("addition+-"), true, Value::subtract);
        addBinaryOperator("*", precedence.get("multiplication*/%"), true, Value::multiply);
        addBinaryOperator("/", precedence.get("multiplication*/%"), true, Value::divide);
        addBinaryOperator("%", precedence.get("multiplication*/%"), true, (v1, v2) ->
                new NumericValue(getNumericalValue(v1).remainder(getNumericalValue(v2), mc)));
        addBinaryOperator("^", precedence.get("exponent^"), false, (v1, v2) ->
        {
            BigDecimal d1 = getNumericalValue(v1);
            BigDecimal d2 = getNumericalValue(v2);
            /*-
             * Thanks to Gene Marin:
             * http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java
             */
            int signOf2 = d2.signum();
            double dn1 = d1.doubleValue();
            d2 = d2.multiply(new BigDecimal(signOf2)); // n2 is now positive
            BigDecimal remainderOf2 = d2.remainder(BigDecimal.ONE);
            BigDecimal n2IntPart = d2.subtract(remainderOf2);
            BigDecimal intPow = d1.pow(n2IntPart.intValueExact(), mc);
            BigDecimal doublePow = new BigDecimal(Math.pow(dn1, remainderOf2.doubleValue()));
            BigDecimal result = intPow.multiply(doublePow, mc);
            if (signOf2 == -1)
                result = BigDecimal.ONE.divide(result, mc.getPrecision(), RoundingMode.HALF_UP);
            return new NumericValue(result);
        });

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
                v1.compareTo(v2) == 0 ? Value.TRUE : Value.FALSE);
        addBinaryOperator("!=", precedence.get("equal==!="), false, (v1, v2) ->
                v1.compareTo(v2) == 0 ? Value.TRUE : Value.FALSE);

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
                     c.setVariable(lname, (cc, tt) -> ri.next().boundTo(lname));
                 }
                 return (cc, tt) -> Value.TRUE;
            }
            v1.assertAssignable();
            String varname = v1.getVariable();
            Value boundedLHS = v2.boundTo(varname);
            c.setVariable(varname, (cc, tt) -> boundedLHS);
            return (cc, tt) -> boundedLHS;
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

        addUnaryOperator("-",  false, (v) -> new NumericValue(getNumericalValue(v).multiply(new BigDecimal(-1))));

        addUnaryOperator("+", false, (v) ->
        {
            getNumericalValue(v);
            return v;
        });
        addUnaryOperator("!", false, (v)-> v.getBoolean() ? Value.FALSE : Value.TRUE); // might need context boolean
        addUnaryFunction("not", (v) -> v.getBoolean() ? Value.FALSE : Value.TRUE);



        addUnaryFunction("fact", (v) ->
        {
            BigDecimal d1 = getNumericalValue(v);
            int number = d1.intValue();
            BigDecimal factorial = BigDecimal.ONE;
            for (int i = 1; i <= number; i++)
            {
                factorial = factorial.multiply(new BigDecimal(i));
            }
            return new NumericValue(factorial);
        });

        addMathematicalUnaryFunction("rand", (d) -> d*randomizer.nextFloat());
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
        addMathematicalUnaryFunction("log", Math::log);
        addMathematicalUnaryFunction("log10", Math::log10);
        addMathematicalUnaryFunction("log1p", Math::log1p);
        addMathematicalUnaryFunction("sqrt", Math::sqrt);

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

        addUnaryFunction("abs", (v) -> new NumericValue(getNumericalValue(v).abs(mc)));
        addBinaryFunction("round", (v1, v2) ->
        {
            BigDecimal toRound = getNumericalValue(v1);
            int precision = getNumericalValue(v2).intValue();
            return new NumericValue(toRound.setScale(precision, mc.getRoundingMode()));
        });
        addUnaryFunction("floor", (v) -> new NumericValue(getNumericalValue(v).setScale(0, RoundingMode.FLOOR)));
        addUnaryFunction("ceil", (v) -> new NumericValue(getNumericalValue(v).setScale(0, RoundingMode.CEILING)));
        addUnaryFunction("relu", (v) -> v.compareTo(Value.ZERO) < 0 ? Value.ZERO : v);
        addUnaryFunction("print", (v) ->
        {
            System.out.println(v.getString());
            return v; // pass through for variables
        });
        addUnaryFunction("return", (v) -> { throw new ExitStatement(v); });

        addFunction("l", ListValue::new);

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
            global_functions.remove(varname);
            global_variables.remove(varname);
            c.variables.remove(varname);
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
                for (String k: c.variables.keySet())
                {
                    if (k.startsWith(prefix))
                        values.add(new StringValue(k));
                }
            }
            return (cc, tt) -> new ListValue(values);
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

        // loop(Num or list, expr, exit_condition) => last_value
        // loop(list, expr,
        // expr receives bounded variable '_' indicating iteration
        addLazyFunction("loop", 2, (c, t, lv) ->
        {
            long limit = getNumericalValue(lv.get(0).evalValue(c)).longValue();
            Value lastOne = Value.ZERO;
            LazyValue expr = lv.get(1);
            //scoping
            LazyValue _val = c.getVariable("_");
            for (long i=0; i < limit; i++)
            {
                long whyYouAsk = i;
                c.setVariable("_", (cc, tt) -> new NumericValue(whyYouAsk).boundTo("_"));
                lastOne = expr.evalValue(c);
            }
            //revering scope
            c.setVariable("_", _val);
            Value trulyLastOne = lastOne;
            return (cc, tt) -> trulyLastOne;
        });

        // map(list or Num, expr, exit_cond) => list_results
        // receives bounded variable '_' with the expression
        addLazyFunction("map", 2, (c, t, lv) ->
        {
            LazyValue expr = lv.get(0);
            Value rval= lv.get(1).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("Second argument of map function should be a list");
            List<Value> list = ((ListValue) rval).getItems();
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; i< list.size(); i++)
            {
                int doYouReally = i;
                c.setVariable("_", (cc, tt) -> list.get(doYouReally).boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(doYouReally).boundTo("_i"));
                result.add(expr.evalValue(c));
            }
            LazyValue ret = (cc, tt) -> new ListValue(result);
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return ret;
        });

        // grep(list or num, expr, exit_expr) => list
        // receives bounded variable '_' with the expression, and "_i" with index
        // produces list of values for which the expression is true
        addLazyFunction("grep", 2, (c, t, lv) ->
        {
            LazyValue expr = lv.get(0);
            Value rval= lv.get(1).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("Second argument of grep function should be a list");
            List<Value> list = ((ListValue) rval).getItems();
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            List<Value> result = new ArrayList<>();
            for (int i=0; i< list.size(); i++)
            {
                int seriously = i;
                c.setVariable("_", (cc, tt) -> list.get(seriously).boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(expr.evalValue(c).getBoolean())
                    result.add(list.get(i));
            }
            LazyValue ret = (cc, tt) -> new ListValue(result);
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
            LazyValue expr = lv.get(0);
            Value rval= lv.get(1).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("Second argument of grep function should be a list");
            List<Value> list = ((ListValue) rval).getItems();
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            for (int i=0; i< list.size(); i++)
            {
                int seriously = i;
                c.setVariable("_", (cc, tt) -> list.get(seriously).boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(expr.evalValue(c).getBoolean())
                {
                    int iFinal = i;
                    c.setVariable("_", _val);
                    c.setVariable("_i", _iter);
                    return (cc, tt) -> list.get(iFinal);
                }
            }
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return LazyValue.NULL;
        });


        // all(list, expr) => boolean
        // receives bounded variable '_' with the expression, and "_i" with index
        // returns true if expr is true for all items
        addLazyFunction("all", 2, (c, t, lv) ->
        {
            LazyValue expr = lv.get(0);
            Value rval= lv.get(1).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("Second argument of grep function should be a list");
            List<Value> list = ((ListValue) rval).getItems();
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            for (int i=0; i< list.size(); i++)
            {
                int seriously = i;
                c.setVariable("_", (cc, tt) -> list.get(seriously).boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(!expr.evalValue(c).getBoolean())
                {
                    c.setVariable("_", _val);
                    c.setVariable("_i", _iter);
                    return (cc, tt) -> new NumericValue(0);
                }
            }
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            return (cc, tt) -> new NumericValue(1);
        });


        // similar to map, but returns total number of successes
        // for(list or num, expr, exit_expr) => success_count
        // can be substituted for first and all, but first is more efficient and all doesn't require knowing list size
        addLazyFunction("for", 2, (c, t, lv) ->
        {
            LazyValue expr = lv.get(0);
            Value rval= lv.get(1).evalValue(c);
            if (!(rval instanceof ListValue))
                throw new InternalExpressionException("Second argument of for function should be a list");
            List<Value> list = ((ListValue) rval).getItems();
            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _iter = c.getVariable("_i");
            int successCount = 0;
            for (int i=0; i< list.size(); i++)
            {
                int seriously = i;
                c.setVariable("_", (cc, tt) -> list.get(seriously).boundTo("_"));
                c.setVariable("_i", (cc, tt) -> new NumericValue(seriously).boundTo("_i"));
                if(expr.evalValue(c).getBoolean())
                    successCount++;
            }
            //revering scope
            c.setVariable("_", _val);
            c.setVariable("_i", _iter);
            long promiseWontChange = successCount;
            return (cc, tt) -> new NumericValue(promiseWontChange);
        });

        //condition and expression will get a bound 'i'
        //returns last successful expression or false
        // while(cond, limit, expr) => ??
        //replaced with for
        addLazyFunction("while", 3, (c, t, lv) ->
        {
            long limit = getNumericalValue(lv.get(1).evalValue(c)).longValue();
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
                throw new InternalExpressionException("Second argument of for function should be a list");
            List<Value> elements= ((ListValue) rval).getItems();

            if (elements.isEmpty())
            {
                Value seriouslyWontChange = acc;
                return (cc, tt) -> seriouslyWontChange;
            }

            //scoping
            LazyValue _val = c.getVariable("_");
            LazyValue _acc = c.getVariable("_a");

            for (Value v: elements)
            {
                Value promiseWontChangeYou = acc;
                c.setVariable("_a", (cc, tt) -> promiseWontChangeYou.boundTo("_a"));
                c.setVariable("_", (cc, tt) -> v.boundTo("_"));
                acc = expr.evalValue(c);
            }
            //reverting scope
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
    private List<Token> shuntingYard(String expression)
    {
        List<Token> outputQueue = new ArrayList<>();
        Stack<Token> stack = new Stack<>();

        Tokenizer tokenizer = new Tokenizer(this, expression);

        Token lastFunction = null;
        Token previousToken = null;
        while (tokenizer.hasNext())
        {
            Token token;
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
                            previousToken.type == Token.TokenType.LITERAL ||
                                    previousToken.type == Token.TokenType.HEX_LITERAL ||
                                    previousToken.type == Token.TokenType.STRINGPARAM))
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
                    if (previousToken != null && previousToken.type == Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(this, previousToken, "Missing parameter(s) for operator ");
                    }
                    while (!stack.isEmpty() && stack.peek().type != Token.TokenType.OPEN_PAREN)
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
                            && (previousToken.type == Token.TokenType.COMMA || previousToken.type == Token.TokenType.OPEN_PAREN))
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
                    if (previousToken != null && previousToken.type != Token.TokenType.OPERATOR
                            && previousToken.type != Token.TokenType.COMMA && previousToken.type != Token.TokenType.OPEN_PAREN)
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
                        if (previousToken.type == Token.TokenType.LITERAL || previousToken.type == Token.TokenType.CLOSE_PAREN
                                || previousToken.type == Token.TokenType.VARIABLE
                                || previousToken.type == Token.TokenType.HEX_LITERAL)
                        {
                            // Implicit multiplication, e.g. 23(a+b) or (a+b)(a-b)
                            Token multiplication = new Token();
                            multiplication.append("*");
                            multiplication.type = Token.TokenType.OPERATOR;
                            stack.push(multiplication);
                        }
                        // if the ( is preceded by a valid function, then it
                        // denotes the start of a parameter list
                        if (previousToken.type == Token.TokenType.FUNCTION)
                        {
                            outputQueue.add(token);
                        }
                    }
                    stack.push(token);
                    break;
                case CLOSE_PAREN:
                    if (previousToken != null && previousToken.type == Token.TokenType.OPERATOR)
                    {
                        throw new ExpressionException(this, previousToken, "Missing parameter(s) for operator " + previousToken);
                    }
                    while (!stack.isEmpty() && stack.peek().type != Token.TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        throw new ExpressionException("Mismatched parentheses");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && stack.peek().type == Token.TokenType.FUNCTION)
                    {
                        outputQueue.add(stack.pop());
                    }
            }
            previousToken = token;
        }

        while (!stack.isEmpty())
        {
            Token element = stack.pop();
            if (element.type == Token.TokenType.OPEN_PAREN || element.type == Token.TokenType.CLOSE_PAREN)
            {
                throw new ExpressionException(this, element, "Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private void shuntOperators(List<Token> outputQueue, Stack<Token> stack, ILazyOperator o1)
    {
        Expression.Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null
                && (nextToken.type == Token.TokenType.OPERATOR
                || nextToken.type == Token.TokenType.UNARY_OPERATOR)
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
     */
    public Value eval(Context c)
    {
        if (ast == null)
        {
            ast = getAST();
        }
        try
        {
            return ast.evalValue(c);
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
        for (final Token token : getRPN())
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
                    //String varname = token.surface.toLowerCase(Locale.ROOT);
                    //if (functions.containsKey(varname) || global_functions.containsKey(varname))
                    //{
                    //    throw new ExpressionException("Variable would mask function: " + token);
                    //    // note: this would actually never happen
                    //} undef requires identifier

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
                        return new NumericValue(new BigDecimal(token.surface, mc));
                    });
                    break;
                case STRINGPARAM:
                    stack.push((c, t) -> new StringValue(token.surface) ); // was originally null
                    break;
                case HEX_LITERAL:
                    stack.push((c, t) -> new NumericValue(new BigDecimal(new BigInteger(token.surface.substring(2), 16), mc)));
                    break;
                default:
                    throw new ExpressionException(this, token, "Unexpected token '" + token.surface + "'");
            }
        }
        return stack.pop();
    }



    /**
     * Cached access to the RPN notation of this expression, ensures only one
     * calculation of the RPN per expression instance. If no cached instance
     * exists, a new one will be created and put to the cache.
     *
     * @return The cached RPN instance.
     */
    private List<Token> getRPN()
    {
        if (rpn == null)
        {
            rpn = shuntingYard(this.expression);
            validate(rpn);
        }
        return rpn;
    }

    /**
     * Check that the expression has enough numbers and variables to fit the
     * requirements of the operators and functions, also check for only 1 result
     * stored at the end of the evaluation.
     */
    private void validate(List<Token> rpn)
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

        for (final Token token : rpn)
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

    void setLogOutput(Consumer<String> to)
    {
        logOutput = to;
    }


    /** Fluff section below */
    public static class Context implements Cloneable
    {
        static final int NONE = 0;
        static final int VOID = 1;
        static final int BOOLEAN = 2;
        static final int NUMBER = 3;
        static final int STRING = 4;
        static final int LIST = 5;
        static final int ITERATOR = 6;
        static final int SIGNATURE = 7;

        public int expected;
        private Map<String, LazyValue> variables = new HashMap<>();
        Consumer<String> logOutput;


        Context(Expression expr, int str)
        {
            expected = str;
            variables.putAll(expr.defaultVariables);
            logOutput = expr.logOutput;
        }

        @Override
        protected Context clone() throws CloneNotSupportedException
        {
            Context clone = (Context) super.clone();
            clone.expected = this.expected;
            clone.variables = this.variables;
            clone.logOutput = this.logOutput;
            return clone;
        }
        Context withExpected(int expectedResult)
        {
            Context other;
            try
            {
                other = this.clone();
            }
            catch (CloneNotSupportedException e1)
            {
                throw new ExpressionException("Clone not supported");
            }
            other.expected = expectedResult;
            return other;
        }


        LazyValue getVariable(String name)
        {
            if (variables.containsKey(name))
            {
                return variables.get(name);
            }
            return global_variables.get(name);
        }

        void setVariable(String name, LazyValue lv)
        {
            if (name.startsWith("global_"))
            {
                global_variables.put(name, lv);
                return;
            }
            variables.put(name, lv);
        }
        void setVariable(String name, Value val)
        {
            setVariable(name, (c, t) -> val.boundTo(name));
        }


        boolean isAVariable(String name)
        {
            return variables.containsKey(name) || global_variables.containsKey(name);
        }


        void delVariable(String variable)
        {
            if (variable.startsWith("global_"))
            {
                global_variables.remove(variable);
                return;
            }
            variables.remove(variable);
        }

        public Context with(String variable, LazyValue lv)
        {
            variables.put(variable, lv);
            return this;
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

    public interface ILazyFunction
    {
        int getNumParams();

        boolean numParamsVaries();

        LazyValue lazyEval(Context c, Integer type, Expression expr, Token token, List<LazyValue> lazyParams);
        // lazy function has a chance to change execution based on contxt
    }

    public interface IFunction extends ILazyFunction
    {
        Value eval(Expression e, Token t, List<Value> parameters);
    }

    public interface ILazyOperator
    {
        int getPrecedence();

        boolean isLeftAssoc();

        LazyValue lazyEval(Context c, Integer type, Expression e, Token t, LazyValue v1, LazyValue v2);
    }

    public interface IOperator extends ILazyOperator
    {
        Value eval(Expression e, Token t, Value v1, Value v2);
    }

    public abstract static class AbstractContextFunction extends AbstractLazyFunction implements ILazyFunction
    {
        protected List<String> arguments;
        protected Expression expression;
        protected Token token;
        AbstractContextFunction(List<String> args, Expression expr, Token t)
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
        public Token getToken()
        {
            return token;
        }



    }

    public abstract static class AbstractLazyFunction implements ILazyFunction
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

    public abstract static class AbstractFunction extends AbstractLazyFunction implements IFunction
    {
        AbstractFunction(int numParams) {
            super(numParams);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Token t, final List<LazyValue> lazyParams) {
            return new LazyValue() { // eager evaluation always ignores the required type and evals params by none default

                private List<Value> params;

                public Value evalValue(Context c, Integer type) {
                    return AbstractFunction.this.eval(e, t, getParams(c));
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
    }

    public abstract static class AbstractLazyOperator implements ILazyOperator
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

    public abstract static class AbstractOperator extends AbstractLazyOperator implements IOperator
    {

        AbstractOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }

        @Override
        public LazyValue lazyEval(Context c_ignored, Integer type, Expression e, Token t, final LazyValue v1, final LazyValue v2) {
            return (c, type_ignored) -> AbstractOperator.this.eval(e, t, v1.evalValue(c), v2.evalValue(c));
        }
    }

    public abstract static class AbstractUnaryOperator extends AbstractOperator
    {
        AbstractUnaryOperator(int precedence, boolean leftAssoc) {
            super(precedence, leftAssoc);
        }

        @Override
        public LazyValue lazyEval(Context cc, Integer type, Expression e, Token t, final LazyValue v1, final LazyValue v2) {
            if (v2 != null) {
                throw new ExpressionException(e, t, "Did not expect a second parameter for unary operator");
            }
            return (c, ignored_type) -> AbstractUnaryOperator.this.evalUnary(e, t, v1.evalValue(c));
        }

        @Override
        public Value eval(Expression e, Token t, Value v1, Value v2) {
            if (v2 != null) {
                throw new ExpressionException(e, t, "Did not expect a second parameter for unary operator");
            }
            return evalUnary(e, t, v1);
        }

        public abstract Value evalUnary(Expression e, Token t, Value v1);
    }
}
