package com.example.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/**
 * Pure Java recursive-descent mathematical expression evaluator supporting
 * basic arithmetic (+, -, *, /, %, ^) and scientific functions (sin, cos, tan, sqrt, log, ln).
 */
public class MathEvaluator {

    private final String input;
    private int pos = -1;
    private int ch;

    private MathEvaluator(String input) {
        this.input = input;
    }

    public static String evaluateFormatted(String expr) throws IllegalArgumentException {
        if (expr == null || expr.trim().isEmpty()) {
            throw new IllegalArgumentException("Empty expression");
        }
        String sanitized = expr
                .replace("×", "*")
                .replace("÷", "/")
                .replace("−", "-")
                .replace("π", String.valueOf(Math.PI))
                .replaceAll("\\s+", "");

        double result = new MathEvaluator(sanitized).parse();
        if (Double.isNaN(result) || Double.isInfinite(result)) {
            throw new IllegalArgumentException("Math error");
        }
        if (Math.abs(result - Math.rint(result)) < 1e-10 && Math.abs(result) < 1e15) {
            return String.format(Locale.US, "%.0f", Math.rint(result));
        }
        BigDecimal bd = BigDecimal.valueOf(result).setScale(8, RoundingMode.HALF_UP).stripTrailingZeros();
        return bd.toPlainString();
    }

    private void nextChar() {
        ch = (++pos < input.length()) ? input.charAt(pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (ch == ' ') nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private double parse() {
        nextChar();
        double x = parseExpression();
        if (pos < input.length()) {
            throw new IllegalArgumentException("Unexpected character: " + (char) ch);
        }
        return x;
    }

    private double parseExpression() {
        double x = parseTerm();
        for (;;) {
            if (eat('+')) x += parseTerm();
            else if (eat('-')) x -= parseTerm();
            else return x;
        }
    }

    private double parseTerm() {
        double x = parseFactor();
        for (;;) {
            if (eat('*')) x *= parseFactor();
            else if (eat('/')) {
                double divisor = parseFactor();
                if (divisor == 0.0) throw new IllegalArgumentException("Divide by zero");
                x /= divisor;
            } else if (eat('%')) {
                double divisor = parseFactor();
                if (divisor == 0.0) throw new IllegalArgumentException("Divide by zero");
                x %= divisor;
            } else {
                return x;
            }
        }
    }

    private double parseFactor() {
        if (eat('+')) return parseFactor();
        if (eat('-')) return -parseFactor();

        double x;
        int startPos = this.pos;
        if (eat('(')) {
            x = parseExpression();
            if (!eat(')')) throw new IllegalArgumentException("Missing ')'");
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {
            while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
            x = Double.parseDouble(input.substring(startPos, this.pos));
        } else if (ch >= 'a' && ch <= 'z') {
            while (ch >= 'a' && ch <= 'z') nextChar();
            String func = input.substring(startPos, this.pos);
            if ("e".equals(func)) {
                x = Math.E;
            } else if ("pi".equals(func)) {
                x = Math.PI;
            } else {
                x = parseFactor();
                switch (func) {
                    case "sqrt":
                        if (x < 0) throw new IllegalArgumentException("Negative sqrt");
                        x = Math.sqrt(x);
                        break;
                    case "sin":
                        x = Math.sin(Math.toRadians(x));
                        break;
                    case "cos":
                        x = Math.cos(Math.toRadians(x));
                        break;
                    case "tan":
                        x = Math.tan(Math.toRadians(x));
                        break;
                    case "log":
                        if (x <= 0) throw new IllegalArgumentException("Log domain");
                        x = Math.log10(x);
                        break;
                    case "ln":
                        if (x <= 0) throw new IllegalArgumentException("Ln domain");
                        x = Math.log(x);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown function: " + func);
                }
            }
        } else {
            throw new IllegalArgumentException("Unexpected token");
        }

        if (eat('^')) x = Math.pow(x, parseFactor());
        return x;
    }
}
