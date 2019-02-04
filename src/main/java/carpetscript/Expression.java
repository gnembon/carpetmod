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
package carpetscript;

import carpet.CarpetSettings;

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
import java.util.Stack;
import java.util.function.Consumer;


/**
 * <h1>EvalEx - Java Expression Evaluator</h1>
 *
 * <h2>Introduction</h2> EvalEx is a handy expression evaluator for Java, that
 * allows to evaluate simple mathematical and boolean expressions. <br>
 * For more information, see:
 * <a href="https://github.com/uklimaschewski/EvalEx">EvalEx GitHub
 * repository</a>
 * <ul>
 * <li>The software is licensed under the MIT Open Source license (see <a href=
 * "https://raw.githubusercontent.com/uklimaschewski/EvalEx/master/LICENSE">LICENSE
 * file</a>).</li>
 * <li>The *power of* operator (^) implementation was copied from <a href=
 * "http://stackoverflow.com/questions/3579779/how-to-do-a-fractional-power-on-bigdecimal-in-java">Stack
 * Overflow</a>. Thanks to Gene Marin.</li>
 * <li>The SQRT() function implementation was taken from the book <a href=
 * "http://www.amazon.de/Java-Number-Cruncher-Programmers-Numerical/dp/0130460419">The
 * Java Programmers Guide To numerical Computing</a> (Ronald Mak, 2002).</li>
 * </ul>
 *
 * @authors Thanks to all who contributed to this project: <a href=
 * "https://github.com/uklimaschewski/EvalEx/graphs/contributors">Contributors</a>
 * @see <a href="https://github.com/uklimaschewski/EvalEx">GitHub repository</a>
 */
public class Expression
{

    /**
     * Unary operators precedence: + and - as prefix
     */
    public static final int OPERATOR_PRECEDENCE_UNARY = 60;

    /**
     * Equality operators precedence: =, ==, !=. <>
     */
    public static final int OPERATOR_PRECEDENCE_EQUALITY = 7;

    /**
     * Comparative operators precedence: <,>,<=,>=
     */
    public static final int OPERATOR_PRECEDENCE_COMPARISON = 10;

    /**
     * Or operator precedence: ||
     */
    public static final int OPERATOR_PRECEDENCE_OR = 3;

    /**
     * And operator precedence: &&
     */
    public static final int OPERATOR_PRECEDENCE_AND = 4;

    /**
     * Power operator precedence: ^
     */
    public static final int OPERATOR_PRECEDENCE_POWER = 40;

    /**
     * Multiplicative operators precedence: *,/,%
     */
    public static final int OPERATOR_PRECEDENCE_MULTIPLICATIVE = 30;

    /**
     * Additive operators precedence: + and -
     */
    public static final int OPERATOR_PRECEDENCE_ADDITIVE = 20;

    /**
     * next command operator: ;
     */
    public static final int OPERATOR_PRECEDENCE_NEXTOP = 1;

    /**
     * Assignment operator precedence: =
     */
    public static final int OPERATOR_PRECEDENCE_ASSIGN = 2;

    /**
     * Definition of PI as a constant, can be used in expressions as variable.
     */
    public static final Value PI = new NumericValue(
            "3.1415926535897932384626433832795028841971693993751058209749445923078164062862089986280348253421170679");


    /**
     * Definition of e: "Euler's number" as a constant, can be used in
     * expressions as variable.
     */
    public static final Value e = new NumericValue(
            "2.71828182845904523536028747135266249775724709369995957496696762772407663");

    /**
     * The {@link MathContext} to use for calculations.
     */
    private MathContext mc;


    /**
     * The current infix expression, with optional variable substitutions.
     */
    private String expression;

    /**
     * The cached RPN (Reverse Polish Notation) of the expression.
     */
    private List<Token> rpn = null;

    /**
     * All defined operators with name and implementation.
     */
    private Map<String, ILazyOperator> operators = new HashMap<>();

    /**
     * All defined functions with name and implementation.
     */
    private Map<String, ILazyFunction> functions = new HashMap<>();

    /**
     * All defined variables with name and value.
     */
    private Map<String, LazyValue> variables = new HashMap<>();

    /**
     * What character to use for decimal separators.
     */
    private static final char decimalSeparator = '.';

    /**
     * What character to use for minus sign (negative values).
     */
    private static final char minusSign = '-';

    private Consumer<String> logOutput = null;

    /**
     * The Value representation of the left parenthesis, used for parsing
     * varying numbers of function parameters.
     */
    private static final LazyValue PARAMS_START = new LazyValue()
    {
        public Value eval()
        {
            return null;
        }

        public String getString()
        {
            return null;
        }
    };

    /**
     * The expression evaluators exception class.
     */
    public static class ExpressionException extends RuntimeException
    {
        //private static final long serialVersionUID = 1118142866870779047L;

        public ExpressionException(String message)
        {
            super(message);
        }
    }

    /**
     * LazyNumber interface created for lazily evaluated functions
     */
    public interface LazyValue
    {
        Value eval();

        String getString();
    }


    /**
     * Construct a LazyNumber from a Value
     */
    public static LazyValue CrazyLazyValue(final Value value)
    {
        return new LazyValue()
        {
            @Override
            public String getString()
            {
                return value.getString();
            }

            @Override
            public Value eval()
            {
                return value;
            }
        };
    }

    public Value evaluateUnaryMathFunction(List<Value> arguments,
                                           java.util.function.Function<Double, Double> fun)
    {
        BigDecimal v = assertNumberNotNull(arguments.get(0));
        double res = fun.apply(v.doubleValue());
        return new NumericValue(new BigDecimal(res, mc));
    }

    public void addMathematicalUnaryFunction(String name, java.util.function.Function<Double, Double> fun)
    {
        addFunction(new Function(name, 1)
        {
            @Override
            public Value eval(List<Value> arguments)
            {
                BigDecimal v = assertNumberNotNull(arguments.get(0));
                double res = fun.apply(v.doubleValue());
                return new NumericValue(new BigDecimal(res, mc));
            }
        });
    }


    public Value evaluateBinaryMathFunction(List<Value> arguments,
                                            java.util.function.BiFunction<Double, Double, Double> fun)
    {
        BigDecimal v1 = assertNumberNotNull(arguments.get(0));
        BigDecimal v2 = assertNumberNotNull(arguments.get(1));
        double res = fun.apply(v1.doubleValue(), v2.doubleValue());
        return new NumericValue(new BigDecimal(res, mc));
    }


    public static LazyValue FALSE = CrazyLazyValue(Value.FALSE);
    public static LazyValue TRUE = CrazyLazyValue(Value.TRUE);
    public static LazyValue EMPTY = CrazyLazyValue(Value.EMPTY);
    public static LazyValue ZERO = CrazyLazyValue(Value.ZERO);

    public abstract class LazyFunction extends AbstractLazyFunction
    {
        /**
         * Creates a new function with given name and parameter count.
         *
         * @param name            The name of the function.
         * @param numParams       The number of parameters for this function.
         *                        <code>-1</code> denotes a variable number of parameters.
         * @param booleanFunction Whether this function is a boolean function.
         */
        public LazyFunction(String name, int numParams, boolean booleanFunction)
        {
            super(name, numParams, booleanFunction);
        }

        /**
         * Creates a new function with given name and parameter count.
         *
         * @param name      The name of the function.
         * @param numParams The number of parameters for this function.
         *                  <code>-1</code> denotes a variable number of parameters.
         */
        public LazyFunction(String name, int numParams)
        {
            super(name, numParams);
        }
    }

    /**
     * Abstract definition of a supported expression function. A function is
     * defined by a name, the number of parameters and the actual processing
     * implementation.
     */
    public abstract class Function extends AbstractFunction
    {

        public Function(String name, int numParams)
        {
            super(name, numParams);
        }

        public Function(String name, int numParams, boolean booleanFunction)
        {
            super(name, numParams, booleanFunction);
        }
    }

    /**
     * Abstract definition of a supported operator. An operator is defined by
     * its name (pattern), precedence and if it is left- or right associative.
     */
    public abstract class Operator extends AbstractOperator
    {

        /**
         * Creates a new operator.
         *
         * @param oper            The operator name (pattern).
         * @param precedence      The operators precedence.
         * @param leftAssoc       <code>true</code> if the operator is left associative,
         *                        else <code>false</code>.
         * @param booleanOperator Whether this operator is boolean.
         */
        public Operator(String oper, int precedence, boolean leftAssoc, boolean booleanOperator)
        {
            super(oper, precedence, leftAssoc, booleanOperator);
        }

        /**
         * Creates a new operator.
         *
         * @param oper       The operator name (pattern).
         * @param precedence The operators precedence.
         * @param leftAssoc  <code>true</code> if the operator is left associative,
         *                   else <code>false</code>.
         */
        public Operator(String oper, int precedence, boolean leftAssoc)
        {
            super(oper, precedence, leftAssoc);
        }
    }

    public abstract class UnaryOperator extends AbstractUnaryOperator
    {

        public UnaryOperator(String oper, int precedence, boolean leftAssoc)
        {
            super(oper, precedence, leftAssoc);
        }
    }

    enum TokenType
    {
        VARIABLE, FUNCTION, LITERAL, OPERATOR, UNARY_OPERATOR, OPEN_PAREN, COMMA, CLOSE_PAREN, HEX_LITERAL, STRINGPARAM
    }

    class Token
    {
        public String surface = "";
        public TokenType type;
        public int pos;

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
    private class Tokenizer implements Iterator<Token>
    {

        /**
         * Actual position in expression string.
         */
        private int pos = 0;

        /**
         * The original input expression.
         */
        private String input;
        /**
         * The previous token or <code>null</code> if none.
         */
        private Token previousToken;

        /**
         * Creates a new tokenizer for an expression.
         *
         * @param input The expression string.
         */
        public Tokenizer(String input)
        {
            this.input = input.trim();
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
            if (pos < (input.length() - 1))
            {
                return input.charAt(pos + 1);
            }
            else
            {
                return 0;
            }
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
                ch = input.charAt(++pos);
            }
            token.pos = pos;

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
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                token.type = isHex ? TokenType.HEX_LITERAL : TokenType.LITERAL;
            }
            else if (ch == '\'')
            {
                pos++;
                if (previousToken == null || previousToken.type != TokenType.STRINGPARAM)
                {
                    ch = input.charAt(pos);
                    while (ch != '\'')
                    {
                        token.append(input.charAt(pos++));
                        ch = pos == input.length() ? 0 : input.charAt(pos);
                    }
                    pos++;
                    token.type = TokenType.STRINGPARAM;
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
                    ch = pos == input.length() ? 0 : input.charAt(pos);
                }
                // Remove optional white spaces after function or variable name
                if (Character.isWhitespace(ch))
                {
                    while (Character.isWhitespace(ch) && pos < input.length())
                    {
                        ch = input.charAt(pos++);
                    }
                    pos--;
                }
                token.type = ch == '(' ? TokenType.FUNCTION : TokenType.VARIABLE;
            }
            else if (ch == '(' || ch == ')' || ch == ',')
            {
                if (ch == '(')
                {
                    token.type = TokenType.OPEN_PAREN;
                }
                else if (ch == ')')
                {
                    token.type = TokenType.CLOSE_PAREN;
                }
                else
                {
                    token.type = TokenType.COMMA;
                }
                token.append(ch);
                pos++;
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
                    if (operators.containsKey(greedyMatch))
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

                if (previousToken == null || previousToken.type == TokenType.OPERATOR
                        || previousToken.type == TokenType.OPEN_PAREN || previousToken.type == TokenType.COMMA)
                {
                    token.surface += "u";
                    token.type = TokenType.UNARY_OPERATOR;
                }
                else
                {
                    token.type = TokenType.OPERATOR;
                }
            }
            return previousToken = token;
        }

        @Override
        public void remove()
        {
            throw new ExpressionException("remove() not supported");
        }

    }

    /**
     * Creates a new expression instance from an expression string with a given
     * default match context of {@link MathContext#DECIMAL32}.
     *
     * @param expression The expression. E.g. <code>"2.4*sin(3)/(2-4)"</code> or
     *                   <code>"sin(y)>0 & max(z, 3)>3"</code>
     */
    public Expression(String expression)
    {
        this(expression, MathContext.DECIMAL32);
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
        this.expression = expression;
        variables.put("e", CrazyLazyValue(e));
        variables.put("PI", CrazyLazyValue(PI));
        variables.put("NULL", null);
        variables.put("TRUE", CrazyLazyValue(Value.TRUE));
        variables.put("FALSE", CrazyLazyValue(Value.FALSE));

        addOperator(new Operator(";", OPERATOR_PRECEDENCE_NEXTOP, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                if (logOutput != null)
                    logOutput.accept(v1.getString());
                return v2;
            }
        });

        addOperator(new Operator("+", OPERATOR_PRECEDENCE_ADDITIVE, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.add(v2);
            }
        });

        addOperator(new Operator("-", OPERATOR_PRECEDENCE_ADDITIVE, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.subtract(v2);
            }
        });


        addOperator(new Operator("*", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.multiply(v2);
            }
        });

        addOperator(new Operator("/", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.divide(v2);
            }
        });

        addOperator(new Operator("%", OPERATOR_PRECEDENCE_MULTIPLICATIVE, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                BigDecimal d1 = assertNumberNotNull(v1);
                BigDecimal d2 = assertNumberNotNull(v2);
                return new NumericValue(d1.remainder(d2, mc));
            }
        });
        addOperator(new Operator("^", OPERATOR_PRECEDENCE_POWER, false)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                BigDecimal d1 = assertNumberNotNull(v1);
                BigDecimal d2 = assertNumberNotNull(v2);
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
                {
                    result = BigDecimal.ONE.divide(result, mc.getPrecision(), RoundingMode.HALF_UP);
                }
                return new NumericValue(result);
            }
        });
        addOperator(new AbstractLazyOperator("&&", OPERATOR_PRECEDENCE_AND, false, true)
        {
            @Override
            public LazyValue eval(LazyValue v1, LazyValue v2)
            {
                assertNotNull(v1);
                boolean b1 = v1.eval().getBoolean();
                if (!b1) return FALSE;
                assertNotNull(v2);
                boolean b2 = v2.eval().getBoolean();
                return b2 ? TRUE : FALSE;
            }
        });

        addOperator(new AbstractLazyOperator("||", OPERATOR_PRECEDENCE_OR, false, true)
        {
            @Override
            public LazyValue eval(LazyValue v1, LazyValue v2)
            {
                assertNotNull(v1);
                boolean b1 = v1.eval().getBoolean();
                if (b1) return TRUE;
                assertNotNull(v2);
                boolean b2 = v2.eval().getBoolean();
                return b2 ? TRUE : FALSE;
            }
        });

        addOperator(new Operator(">", OPERATOR_PRECEDENCE_COMPARISON, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) > 0 ? Value.TRUE : Value.FALSE;
            }
        });

        addOperator(new Operator(">=", OPERATOR_PRECEDENCE_COMPARISON, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) >= 0 ? Value.TRUE : Value.FALSE;
            }
        });

        addOperator(new Operator("<", OPERATOR_PRECEDENCE_COMPARISON, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) < 0 ? Value.TRUE : Value.FALSE;
            }
        });

        addOperator(new Operator("<=", OPERATOR_PRECEDENCE_COMPARISON, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) <= 0 ? Value.TRUE : Value.FALSE;
            }
        });

        // consider replacement with AbstractLazyOperator
        addOperator(new Operator("=", OPERATOR_PRECEDENCE_ASSIGN, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                if (!v1.isBound())
                {
                    throw new ExpressionException("LHS of assignment needs to be a variable");
                }
                String varname = v1.getVariable();
                Value boundedLHS = v2.boundTo(varname);
                LazyValue lazyLHS = CrazyLazyValue(boundedLHS);
                variables.put(varname, lazyLHS);
                return boundedLHS;
            }
        });

        addOperator(new Operator("==", OPERATOR_PRECEDENCE_EQUALITY, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) == 0 ? Value.TRUE : Value.FALSE;
            }
        });

        addOperator(new Operator("!=", OPERATOR_PRECEDENCE_EQUALITY, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                return v1.compareTo(v2) == 0 ? Value.TRUE : Value.FALSE;
            }
        });

        addOperator(new Operator("<>", OPERATOR_PRECEDENCE_EQUALITY, false, true)
        {
            @Override
            public Value eval(Value v1, Value v2)
            {
                assertNotNull(v1, v2);
                if (!v1.isBound() || !v2.isBound())
                {
                    throw new ExpressionException("Both sides of swapping assignment need to be variables");
                }
                String lvalvar = v1.getVariable();
                String rvalvar = v2.getVariable();
                Value lval = v2.boundTo(lvalvar);
                Value rval = v1.boundTo(rvalvar);
                LazyValue lazyLHS = CrazyLazyValue(lval);
                LazyValue lazyRHS = CrazyLazyValue(rval);
                variables.put(lvalvar, lazyLHS);
                variables.put(rvalvar, lazyRHS);
                return lval;
            }
        });

        addOperator(new UnaryOperator("-", OPERATOR_PRECEDENCE_UNARY, false)
        {
            @Override
            public Value evalUnary(Value v1)
            {
                BigDecimal d1 = assertNumberNotNull(v1);
                return new NumericValue(d1.multiply(new BigDecimal(-1)));
            }
        });

        addOperator(new UnaryOperator("+", OPERATOR_PRECEDENCE_UNARY, false)
        {
            @Override
            public Value evalUnary(Value v1)
            {
                assertNumberNotNull(v1);
                return v1;
            }
        });

        addFunction(new Function("FACT", 1, false)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                BigDecimal d1 = assertNumberNotNull(parameters.get(0));
                int number = d1.intValue();
                BigDecimal factorial = BigDecimal.ONE;
                for (int i = 1; i <= number; i++)
                {
                    factorial = factorial.multiply(new BigDecimal(i));
                }
                return new NumericValue(factorial);
            }
        });

        addFunction(new Function("NOT", 1, true)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                assertNotNull(parameters.get(0));
                boolean b = parameters.get(0).getBoolean();
                return b ? Value.FALSE : Value.TRUE;
            }
        });

        addLazyFunction(new LazyFunction("IF", 3)
        {
            @Override
            public LazyValue lazyEval(List<LazyValue> lazyParams)
            {
                Value result = lazyParams.get(0).eval();
                assertNotNull(result);
                boolean isTrue = result.getBoolean();
                return isTrue ? lazyParams.get(1) : lazyParams.get(2);
            }
        });


        addFunction(new Function("RANDOM", 0)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, (d) -> Math.random());
            }
        });

        addFunction(new Function("SIN", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, (d) -> Math.sin(Math.toRadians(d)));
            }
        });

        addFunction(new Function("COS", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, (d) -> Math.cos(Math.toRadians(d)));
            }
        });
        addFunction(new Function("TAN", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, (d) -> Math.tan(Math.toRadians(d)));
            }
        });
        addFunction(new Function("ASIN", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, (d) -> Math.toDegrees(Math.asin(d)));
            }
        });
        addFunction(new Function("ACOS", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, (d) -> Math.toDegrees(Math.acos(d)));
            }
        });
        addFunction(new Function("ATAN", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, (d) -> Math.toDegrees(Math.atan(d)));
            }
        });
        addFunction(new Function("ATAN2", 2)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateBinaryMathFunction(parameters, (d, d2) -> Math.toDegrees(Math.atan2(d, d2)));
            }
        });
        addFunction(new Function("SINH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::sinh);
            }
        });
        addFunction(new Function("COSH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::cosh);
            }
        });
        addFunction(new Function("TANH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::tanh);
            }
        });
        addFunction(new Function("SEC", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: sec(x) = 1 / cos(x) */
                return evaluateUnaryMathFunction(parameters, (d) -> 1.0 / Math.cos(Math.toRadians(d)));
            }
        });
        addFunction(new Function("CSC", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: csc(x) = 1 / sin(x) */
                return evaluateUnaryMathFunction(parameters, (d) -> 1.0 / Math.sin(Math.toRadians(d)));
            }
        });
        addFunction(new Function("SECH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: sech(x) = 1 / cosh(x) */
                return evaluateUnaryMathFunction(parameters, (d) -> 1.0 / Math.cosh(d));
            }
        });
        addFunction(new Function("CSCH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: csch(x) = 1 / sinh(x) */
                return evaluateUnaryMathFunction(parameters, (d) -> 1.0 / Math.sinh(d));
            }
        });
        addFunction(new Function("COT", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: cot(x) = cos(x) / sin(x) = 1 / tan(x) */
                return evaluateUnaryMathFunction(parameters, (d) -> 1.0 / Math.tan(Math.toRadians(d)));
            }
        });
        addFunction(new Function("ACOT", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: acot(x) = atan(1/x) */
                return evaluateUnaryMathFunction(parameters, (d) -> Math.toDegrees(Math.atan(1.0 / d)));
            }
        });
        addFunction(new Function("COTH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: coth(x) = 1 / tanh(x) */
                return evaluateUnaryMathFunction(parameters, (d) -> 1.0 / Math.tanh(d));
            }
        });
        addFunction(new Function("ASINH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: asinh(x) = ln(x + sqrt(x^2 + 1)) */
                return evaluateUnaryMathFunction(parameters, (d) -> Math.log(d + (Math.sqrt(Math.pow(d, 2) + 1))));
            }
        });
        addFunction(new Function("ACOSH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: acosh(x) = ln(x + sqrt(x^2 - 1)) */
                return evaluateUnaryMathFunction(parameters, (d) -> Math.log(d + (Math.sqrt(Math.pow(d, 2) - 1))));
            }
        });
        addFunction(new Function("ATANH", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            { /* Formula: atanh(x) = 0.5*ln((1 + x)/(1 - x)) */
                return evaluateUnaryMathFunction(parameters, (d) ->
                {
                    if (Math.abs(d) > 1 || Math.abs(d) == 1)
                    {
                        throw new ExpressionException("Number must be |x| < 1");
                    }
                    return 0.5 * Math.log((1 + d) / (1 - d));
                });
            }
        });
        addFunction(new Function("RAD", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::toRadians);
            }
        });
        addFunction(new Function("DEG", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::toDegrees);
            }
        });
        addFunction(new Function("MAX", -1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                if (parameters.size() == 0)
                    throw new ExpressionException("MAX requires at least one parameter");
                Value max = null;
                for (Value parameter : parameters)
                    if (max == null || parameter.compareTo(max) > 0)
                        max = parameter;
                return max;
            }
        });
        addFunction(new Function("MIN", -1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                if (parameters.size() == 0)
                {
                    throw new ExpressionException("MIN requires at least one parameter");
                }
                Value min = null;
                for (Value parameter : parameters)
                {
                    if (min == null || parameter.compareTo(min) < 0)
                        min = parameter;
                }
                return min;
            }
        });
        addFunction(new Function("ABS", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                BigDecimal d = assertNumberNotNull(parameters.get(0));
                return new NumericValue(d.abs(mc));
            }
        });
        addFunction(new Function("LOG", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::log);
            }
        });
        addFunction(new Function("LOG10", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::log10);
            }
        });
        addFunction(new Function("ROUND", 2)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                BigDecimal toRound = assertNumberNotNull(parameters.get(0));
                int precision = assertNumberNotNull(parameters.get(1)).intValue();
                return new NumericValue(toRound.setScale(precision, mc.getRoundingMode()));
            }
        });
        addFunction(new Function("FLOOR", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                BigDecimal toRound = assertNumberNotNull(parameters.get(0));
                return new NumericValue(toRound.setScale(0, RoundingMode.FLOOR));
            }
        });
        addFunction(new Function("CEILING", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                BigDecimal toRound = assertNumberNotNull(parameters.get(0));
                return new NumericValue(toRound.setScale(0, RoundingMode.CEILING));
            }
        });
        addFunction(new Function("SQRT", 1)
        {
            @Override
            public Value eval(List<Value> parameters)
            {
                return evaluateUnaryMathFunction(parameters, Math::sqrt);
            }
        });

        addFunction(new AbstractFunction("relu", 1, false)
        {  // why not
            @Override
            public Value eval(List<Value> params)
            {
                if (params.get(0).compareTo(Value.ZERO) < 0)
                    return Value.ZERO;
                return params.get(0);
            }
        });

        addFunction(new AbstractFunction("print", 1)
        {
            @Override
            public Value eval(List<Value> params)
            {
                Value arg = params.get(0);
                System.out.println(arg.getString());
                return arg; // pass through for variables
            }
        });

        addFunction(new AbstractFunction("list", -1)
        {
            @Override
            public Value eval(List<Value> params)
            {
                return new ListValue(params);
            }
        });

        addFunction(new AbstractFunction("range", 1)
        {
            @Override
            public Value eval(List<Value> params)
            {
                throw new UnsupportedOperationException(); // TODO
            }
        });

        addFunction(new AbstractFunction("for", 1)
        {
            @Override
            public Value eval(List<Value> params)
            {
                throw new UnsupportedOperationException(); // TODO
            }
        });

        addFunction(new AbstractFunction("map", 2)
        {
            @Override
            public Value eval(List<Value> params)
            {
                throw new UnsupportedOperationException(); // TODO
            }
        });

        addFunction(new AbstractFunction("reduce", -1)
        { // 2 or 3
            @Override
            public Value eval(List<Value> params)
            {
                throw new UnsupportedOperationException(); // TODO
            }
        });

        addFunction(new AbstractFunction("case", 1)
        {
            @Override
            public Value eval(List<Value> params)
            {
                throw new UnsupportedOperationException(); // TODO
            }
        });


    }

    private void assertNotNull(Value v1)
    {
        if (v1 == null)
        {
            throw new ExpressionException("Operand may not be null");
        }
    }

    private BigDecimal assertNumberNotNull(Value v1)
    {
        if (v1 == null)
        {
            throw new ExpressionException("Operand may not be null");
        }
        if (!(v1 instanceof NumericValue))
        {
            throw new ExpressionException("Operand has to be of a numeric type");
        }
        return ((NumericValue) v1).getNumber();
    }

    private void assertNotNull(Value v1, Value v2)
    {
        if (v1 == null)
        {
            throw new ExpressionException("First operand may not be null");
        }
        if (v2 == null)
        {
            throw new ExpressionException("Second operand may not be null");
        }
    }


    private void assertNotNull(LazyValue v1)
    {
        if (v1 == null)
        {
            throw new ExpressionException("Operand may not be null");
        }
    }

    /**
     * Is the string a number?
     *
     * @param st The string.
     * @return <code>true</code>, if the input string is a number.
     */
    private boolean isNumber(String st)
    {
        if (st.charAt(0) == minusSign && st.length() == 1)
            return false;
        if (st.charAt(0) == '+' && st.length() == 1)
            return false;
        if (st.charAt(0) == decimalSeparator && (st.length() == 1 || !Character.isDigit(st.charAt(1))))
            return false;
        if (st.charAt(0) == 'e' || st.charAt(0) == 'E')
            return false;
        for (char ch : st.toCharArray())
        {
            if (!Character.isDigit(ch) && ch != minusSign && ch != decimalSeparator && ch != 'e' && ch != 'E'
                    && ch != '+')
                return false;
        }
        return true;
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

        Tokenizer tokenizer = new Tokenizer(expression);

        Token lastFunction = null;
        Token previousToken = null;
        while (tokenizer.hasNext())
        {
            Token token = tokenizer.next();
            switch (token.type)
            {
                case STRINGPARAM:
                    //stack.push(token);
                    //break;
                case LITERAL:
                case HEX_LITERAL:
                    if (previousToken != null && (
                            previousToken.type == TokenType.LITERAL ||
                                    previousToken.type == TokenType.HEX_LITERAL ||
                                    previousToken.type == TokenType.STRINGPARAM))
                    {
                        throw new ExpressionException("Missing operator at character position " + token.pos);
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
                    if (previousToken != null && previousToken.type == TokenType.OPERATOR)
                    {
                        throw new ExpressionException("Missing parameter(s) for operator " + previousToken
                                + " at character position " + previousToken.pos);
                    }
                    while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        if (lastFunction == null)
                        {
                            throw new ExpressionException("Unexpected comma at character position " + token.pos);
                        }
                        else
                        {
                            throw new ExpressionException(
                                    "Parse error for function '" + lastFunction + "' at character position " + token.pos);
                        }
                    }
                    break;
                case OPERATOR:
                {
                    if (previousToken != null
                            && (previousToken.type == TokenType.COMMA || previousToken.type == TokenType.OPEN_PAREN))
                    {
                        throw new ExpressionException(
                                "Missing parameter(s) for operator " + token + " at character position " + token.pos);
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException("Unknown operator '" + token + "' at position " + (token.pos + 1));
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case UNARY_OPERATOR:
                {
                    if (previousToken != null && previousToken.type != TokenType.OPERATOR
                            && previousToken.type != TokenType.COMMA && previousToken.type != TokenType.OPEN_PAREN)
                    {
                        throw new ExpressionException(
                                "Invalid position for unary operator " + token + " at character position " + token.pos);
                    }
                    ILazyOperator o1 = operators.get(token.surface);
                    if (o1 == null)
                    {
                        throw new ExpressionException(
                                "Unknown unary operator '" + token.surface.substring(0, token.surface.length() - 1)
                                        + "' at position " + (token.pos + 1));
                    }

                    shuntOperators(outputQueue, stack, o1);
                    stack.push(token);
                    break;
                }
                case OPEN_PAREN:
                    if (previousToken != null)
                    {
                        if (previousToken.type == TokenType.LITERAL || previousToken.type == TokenType.CLOSE_PAREN
                                || previousToken.type == TokenType.VARIABLE
                                || previousToken.type == TokenType.HEX_LITERAL)
                        {
                            // Implicit multiplication, e.g. 23(a+b) or (a+b)(a-b)
                            Token multiplication = new Token();
                            multiplication.append("*");
                            multiplication.type = TokenType.OPERATOR;
                            stack.push(multiplication);
                        }
                        // if the ( is preceded by a valid function, then it
                        // denotes the start of a parameter list
                        if (previousToken.type == TokenType.FUNCTION)
                        {
                            outputQueue.add(token);
                        }
                    }
                    stack.push(token);
                    break;
                case CLOSE_PAREN:
                    if (previousToken != null && previousToken.type == TokenType.OPERATOR)
                    {
                        throw new ExpressionException("Missing parameter(s) for operator " + previousToken
                                + " at character position " + previousToken.pos);
                    }
                    while (!stack.isEmpty() && stack.peek().type != TokenType.OPEN_PAREN)
                    {
                        outputQueue.add(stack.pop());
                    }
                    if (stack.isEmpty())
                    {
                        throw new ExpressionException("Mismatched parentheses");
                    }
                    stack.pop();
                    if (!stack.isEmpty() && stack.peek().type == TokenType.FUNCTION)
                    {
                        outputQueue.add(stack.pop());
                    }
            }
            previousToken = token;
        }

        while (!stack.isEmpty())
        {
            Token element = stack.pop();
            if (element.type == TokenType.OPEN_PAREN || element.type == TokenType.CLOSE_PAREN)
            {
                throw new ExpressionException("Mismatched parentheses");
            }
            outputQueue.add(element);
        }
        return outputQueue;
    }

    private void shuntOperators(List<Token> outputQueue, Stack<Token> stack, ILazyOperator o1)
    {
        Expression.Token nextToken = stack.isEmpty() ? null : stack.peek();
        while (nextToken != null
                && (nextToken.type == Expression.TokenType.OPERATOR
                || nextToken.type == Expression.TokenType.UNARY_OPERATOR)
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
    public Value eval()
    {
        Stack<LazyValue> stack = new Stack<>();
        for (final Token token : getRPN())
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                {
                    final LazyValue value = stack.pop();
                    LazyValue result = new LazyValue()
                    {
                        public Value eval()
                        {
                            return operators.get(token.surface).eval(value, null).eval();
                        }

                        @Override
                        public String getString()
                        {
                            return String.valueOf(operators.get(token.surface).eval(value, null).eval());
                        }
                    };
                    stack.push(result);
                    break;
                }
                case OPERATOR:
                    final LazyValue v1 = stack.pop();
                    final LazyValue v2 = stack.pop();
                    LazyValue result = new LazyValue()
                    {
                        public Value eval()
                        {
                            return operators.get(token.surface).eval(v2, v1).eval();
                        }

                        public String getString()
                        {
                            return String.valueOf(operators.get(token.surface).eval(v2, v1).eval());
                        }
                    };
                    stack.push(result);
                    break;
                case VARIABLE:
                    if (functions.containsKey(token.surface.toUpperCase(Locale.ROOT)))
                    {
                        throw new ExpressionException("Variable would mask function: " + token);
                    }

                    stack.push(new LazyValue()
                    {
                        public Value eval()
                        {
                            if (!variables.containsKey(token.surface)) // new variable
                            {
                                variables.put(token.surface, CrazyLazyValue(Value.ZERO.boundTo(token.surface)));
                            }
                            LazyValue lazyVariable = variables.get(token.surface);
                            Value value = lazyVariable.eval();
                            return value;
                        }

                        public String getString()
                        {
                            return token.surface;
                        }
                    });
                    break;
                case FUNCTION:
                    ILazyFunction f = functions.get(token.surface.toUpperCase(Locale.ROOT));
                    ArrayList<LazyValue> p = new ArrayList<>(!f.numParamsVaries() ? f.getNumParams() : 0);
                    // pop parameters off the stack until we hit the start of
                    // this function's parameter list
                    while (!stack.isEmpty() && stack.peek() != PARAMS_START)
                    {
                        p.add(0, stack.pop());
                    }

                    if (stack.peek() == PARAMS_START)
                    {
                        stack.pop();
                    }

                    stack.push(new LazyValue()
                    {
                        public Value eval()
                        {
                            return f.lazyEval(p).eval();
                        }

                        public String getString()
                        {
                            return String.valueOf(f.lazyEval(p).eval());
                        }
                    });
                    break;
                case OPEN_PAREN:
                    stack.push(PARAMS_START);
                    break;
                case LITERAL:
                    stack.push(new LazyValue()
                    {
                        public Value eval()
                        {
                            if (token.surface.equalsIgnoreCase("NULL"))
                            {
                                return null;
                            }
                            return new NumericValue(new BigDecimal(token.surface, mc));
                        }

                        public String getString()
                        {
                            return String.valueOf(new BigDecimal(token.surface, mc));
                        }
                    });
                    break;
                case STRINGPARAM:
                    stack.push(new LazyValue()
                    {
                        public Value eval()
                        {
                            return new StringValue(token.surface); // was null
                        }

                        public String getString()
                        {
                            return token.surface;
                        }
                    });
                    break;
                case HEX_LITERAL:
                    stack.push(new LazyValue()
                    {
                        public Value eval()
                        {
                            return new NumericValue(new BigDecimal(new BigInteger(token.surface.substring(2), 16), mc));
                        }

                        public String getString()
                        {
                            return new BigInteger(token.surface.substring(2), 16).toString();
                        }
                    });
                    break;
                default:
                    throw new ExpressionException(
                            "Unexpected token '" + token.surface + "' at character position " + token.pos);
            }
        }
        return stack.pop().eval();
    }

    /**
     * Adds an operator to the list of supported operators.
     *
     * @param operator The operator to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public <OPERATOR extends ILazyOperator> OPERATOR addOperator(OPERATOR operator)
    {
        String key = operator.getOper();
        if (operator instanceof AbstractUnaryOperator)
        {
            key += "u";
        }
        return (OPERATOR) operators.put(key, operator);
    }

    /**
     * Adds a function to the list of supported functions
     *
     * @param function The function to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public IFunction addFunction(IFunction function)
    {
        return (IFunction) functions.put(function.getName(), function);
    }

    /**
     * Adds a lazy function function to the list of supported functions
     *
     * @param function The function to add.
     * @return The previous operator with that name, or <code>null</code> if
     * there was none.
     */
    public ILazyFunction addLazyFunction(ILazyFunction function)
    {
        return functions.put(function.getName(), function);
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable name.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression setVariable(String variable, Value value)
    {
        return setVariable(variable, CrazyLazyValue(value.boundTo(variable)));
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable name.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression setVariable(String variable, LazyValue value)
    {
        variables.put(variable, value);
        return this;
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression setVariable(String variable, String value)
    {
        if (isNumber(value))
            variables.put(variable, CrazyLazyValue(new NumericValue(new BigDecimal(value, mc)).boundTo(variable)));
        else if (value.equalsIgnoreCase("null"))
        {
            variables.put(variable, null);
        }
        else
        {
            variables.put(variable, CrazyLazyValue(new StringValue(value).boundTo(variable)));
        }
        return this;
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression with(String variable, BigDecimal value)
    {
        return setVariable(variable, new NumericValue(value)); // variable will be set by setVariable
    }

    /**
     * Sets a variable value.
     *
     * @param variable The variable to set.
     * @param value    The variable value.
     * @return The expression, allows to chain methods.
     */
    public Expression with(String variable, String value)
    {
        return setVariable(variable, value);
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
        Stack<Integer> stack = new Stack<Integer>();

        // push the 'global' scope
        stack.push(0);

        for (final Token token : rpn)
        {
            switch (token.type)
            {
                case UNARY_OPERATOR:
                    if (stack.peek() < 1)
                    {
                        throw new ExpressionException("Missing parameter(s) for operator " + token);
                    }
                    break;
                case OPERATOR:
                    if (stack.peek() < 2)
                    {
                        if (token.surface.equalsIgnoreCase(";"))
                        {
                            throw new ExpressionException("Unnecessary semicolon at position " + (token.pos + 1));
                        }
                        throw new ExpressionException("Missing parameter(s) for operator " + token);
                    }
                    // pop the operator's 2 parameters and add the result
                    stack.set(stack.size() - 1, stack.peek() - 2 + 1);
                    break;
                case FUNCTION:
                    ILazyFunction f = functions.get(token.surface.toUpperCase(Locale.ROOT));
                    if (f == null)
                    {
                        throw new ExpressionException("Unknown function '" + token + "' at position " + (token.pos + 1));
                    }

                    int numParams = stack.pop();
                    if (!f.numParamsVaries() && numParams != f.getNumParams())
                    {
                        throw new ExpressionException(
                                "IFunction " + token + " expected " + f.getNumParams() + " parameters, got " + numParams);
                    }
                    if (stack.size() <= 0)
                    {
                        throw new ExpressionException("Too many function calls, maximum scope exceeded");
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

    public void setLogOutput(Consumer<String> to)
    {
        logOutput = to;
    }
}
