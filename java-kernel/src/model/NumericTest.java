import java.math.BigDecimal;

public final class NumericTest {

    private static int assertions = 0;

    public static void main(String[] args) {

        exactParsing();
        precision38();
        precisionOverflow();
        currencyScale();
        rounding();
        scaleGreaterThanPrecision();
        negativeScale();
        addition();
        subtraction();
        multiplication();
        terminatingDivision();
        repeatingDivision();
        nullPropagation();
        divideByZero();
        integerCoercion();
        decimalStringCoercion();
        floatingPointRejected();
        invalidPrecision();
        invalidScale();
        scaleWithoutPrecision();

        System.out.println(
                "NumericTest PASSED: " +
                assertions +
                " assertions");
    }

    private static void exactParsing() {

        assertDecimal(
                "12345678901234567890.123456789",
                Numeric.number(
                        "12345678901234567890.123456789"));
    }

    private static void precision38() {

        BigDecimal value =
                Numeric.enforce(
                        Numeric.number(
                                "99999999999999999999999999999999999999"),
                        38,
                        0);

        assertDecimal(
                "99999999999999999999999999999999999999",
                value);
    }

    private static void precisionOverflow() {

        expectArithmeticFailure(
                () -> Numeric.enforce(
                        Numeric.number(
                                "999999999999999999999999999999999999999"),
                        38,
                        0));
    }

    private static void currencyScale() {

        BigDecimal value =
                Numeric.enforce(
                        Numeric.number("123.4"),
                        18,
                        2);

        assertDecimal(
                "123.40",
                value);

        assertEquals(
                2,
                value.scale());
    }

    private static void rounding() {

        BigDecimal value =
                Numeric.enforce(
                        Numeric.number("123.456"),
                        18,
                        2);

        assertDecimal(
                "123.46",
                value);
    }

    private static void scaleGreaterThanPrecision() {

        BigDecimal value =
                Numeric.enforce(
                        Numeric.number("0.01234"),
                        4,
                        5);

        assertDecimal(
                "0.01234",
                value);

        expectArithmeticFailure(
                () -> Numeric.enforce(
                        Numeric.number("0.12345"),
                        4,
                        5));
    }

    private static void negativeScale() {

        BigDecimal value =
                Numeric.enforce(
                        Numeric.number("12345"),
                        4,
                        -2);

        assertDecimal(
                "12300",
                value);
    }

    private static void addition() {

        assertDecimal(
                "0.3",
                Numeric.add(
                        Numeric.number("0.1"),
                        Numeric.number("0.2")));
    }

    private static void subtraction() {

        assertDecimal(
                "17550",
                Numeric.subtract(
                        Numeric.number("27250"),
                        Numeric.number("9700")));
    }

    private static void multiplication() {

        assertDecimal(
                "30.00",
                Numeric.multiply(
                        Numeric.number("10.00"),
                        Numeric.number("3")));
    }

    private static void terminatingDivision() {

        assertDecimal(
                "0.5",
                Numeric.divide(
                        Numeric.number("20000"),
                        Numeric.number("40000")));
    }

    private static void repeatingDivision() {

        BigDecimal value =
                Numeric.divide(
                        Numeric.number("17550"),
                        Numeric.number("27250"));

        assertTrue(
                value.toPlainString()
                        .startsWith(
                                "0.64403669724770642201834862385321100917"),
                "unexpected repeating division result: " +
                value);
    }

    private static void nullPropagation() {

        assertNull(
                Numeric.add(
                        null,
                        Numeric.number("5")));

        assertNull(
                Numeric.subtract(
                        Numeric.number("5"),
                        null));

        assertNull(
                Numeric.multiply(
                        null,
                        null));

        assertNull(
                Numeric.divide(
                        null,
                        Numeric.number("5")));
    }

    private static void divideByZero() {

        expectArithmeticFailure(
                () -> Numeric.divide(
                        Numeric.number("10"),
                        BigDecimal.ZERO));
    }

    private static void integerCoercion() {

        assertDecimal(
                "42",
                Numeric.coerce(42));

        assertDecimal(
                "9223372036854775807",
                Numeric.coerce(
                        Long.MAX_VALUE));
    }

    private static void decimalStringCoercion() {

        assertDecimal(
                "10.2500",
                Numeric.coerce(
                        "10.2500"));
    }

    private static void floatingPointRejected() {

        expectIllegalArgument(
                () -> Numeric.coerce(0.1d));

        expectIllegalArgument(
                () -> Numeric.coerce(0.1f));
    }

    private static void invalidPrecision() {

        expectIllegalArgument(
                () -> Numeric.validateDeclaration(
                        0,
                        0));

        expectIllegalArgument(
                () -> Numeric.validateDeclaration(
                        39,
                        0));
    }

    private static void invalidScale() {

        expectIllegalArgument(
                () -> Numeric.validateDeclaration(
                        38,
                        -85));

        expectIllegalArgument(
                () -> Numeric.validateDeclaration(
                        38,
                        128));
    }

    private static void scaleWithoutPrecision() {

        expectIllegalArgument(
                () -> Numeric.validateDeclaration(
                        null,
                        2));
    }

    private static void assertDecimal(
            String expected,
            BigDecimal actual) {

        assertions++;

        BigDecimal expectedValue =
                new BigDecimal(expected);

        if (actual == null ||
            expectedValue.compareTo(actual) != 0) {

            throw new AssertionError(
                    "expected decimal " +
                    expected +
                    " but got " +
                    actual);
        }
    }

    private static void assertEquals(
            int expected,
            int actual) {

        assertions++;

        if (expected != actual) {

            throw new AssertionError(
                    "expected " +
                    expected +
                    " but got " +
                    actual);
        }
    }

    private static void assertTrue(
            boolean value,
            String message) {

        assertions++;

        if (!value) {
            throw new AssertionError(message);
        }
    }

    private static void assertNull(
            Object value) {

        assertions++;

        if (value != null) {

            throw new AssertionError(
                    "expected null but got " +
                    value);
        }
    }

    private static void expectArithmeticFailure(
            Runnable action) {

        assertions++;

        try {
            action.run();

            throw new AssertionError(
                    "expected ArithmeticException");
        } catch (ArithmeticException expected) {
            // expected
        }
    }

    private static void expectIllegalArgument(
            Runnable action) {

        assertions++;

        try {
            action.run();

            throw new AssertionError(
                    "expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // expected
        }
    }
}
