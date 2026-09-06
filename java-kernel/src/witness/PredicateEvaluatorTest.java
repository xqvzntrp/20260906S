import java.time.*;
import java.util.*;

public final class PredicateEvaluatorTest {

    public static void main(String[] args) {
        testNullEqualsProducesUnknown();
        testNullNotEqualsProducesUnknown();
        testIsNotNull();
        testNullOrderingProducesUnknown();
        testTextEquality();
        testIntegerEquality();
        testIntegerDecimalComparison();
        testAndTruthTable();

        System.out.println(
                "PredicateEvaluatorTest PASSED");
    }

    private static void testNullEqualsProducesUnknown() {
        require(
                PredicateEvaluator.compare(
                        null,
                        "A",
                        SemanticModel.Type.TEXT,
                        "=",
                        SemanticModel.Type.TEXT)
                == PredicateEvaluator.Truth.UNKNOWN,
                "NULL = value");
    }

    private static void testNullNotEqualsProducesUnknown() {
        require(
                PredicateEvaluator.compare(
                        null,
                        "A",
                        SemanticModel.Type.TEXT,
                        "!=",
                        SemanticModel.Type.TEXT)
                == PredicateEvaluator.Truth.UNKNOWN,
                "NULL != value");
    }

    private static void testIsNotNull() {
        require(
                PredicateEvaluator.compare(
                        null,
                        null,
                        SemanticModel.Type.TEXT,
                        "is_not_null",
                        SemanticModel.Type.TEXT)
                == PredicateEvaluator.Truth.FALSE,
                "NULL is_not_null");

        require(
                PredicateEvaluator.compare(
                        "A",
                        null,
                        SemanticModel.Type.TEXT,
                        "is_not_null",
                        SemanticModel.Type.TEXT)
                == PredicateEvaluator.Truth.TRUE,
                "value is_not_null");
    }

    private static void testNullOrderingProducesUnknown() {
        require(
                PredicateEvaluator.compare(
                        10L,
                        null,
                        SemanticModel.Type.INTEGER,
                        ">",
                        SemanticModel.Type.INTEGER)
                == PredicateEvaluator.Truth.UNKNOWN,
                "value > NULL");
    }

    private static void testTextEquality() {
        require(
                PredicateEvaluator.compare(
                        "A",
                        "A",
                        SemanticModel.Type.TEXT,
                        "=",
                        SemanticModel.Type.TEXT)
                == PredicateEvaluator.Truth.TRUE,
                "TEXT equality");

        require(
                PredicateEvaluator.compare(
                        "A",
                        "B",
                        SemanticModel.Type.TEXT,
                        "=",
                        SemanticModel.Type.TEXT)
                == PredicateEvaluator.Truth.FALSE,
                "TEXT inequality");
    }

    private static void testIntegerEquality() {
        require(
                PredicateEvaluator.compare(
                        10L,
                        10L,
                        SemanticModel.Type.INTEGER,
                        "=",
                        SemanticModel.Type.INTEGER)
                == PredicateEvaluator.Truth.TRUE,
                "INTEGER equality");
    }

    private static void testIntegerDecimalComparison() {
        require(
                PredicateEvaluator.compare(
                        10L,
                        new java.math.BigDecimal("10.0"),
                        SemanticModel.Type.INTEGER,
                        "=",
                        SemanticModel.Type.DECIMAL)
                == PredicateEvaluator.Truth.TRUE,
                "INTEGER DECIMAL equality");
    }

    private static void testAndTruthTable() {
        require(
                PredicateEvaluator.and(
                        PredicateEvaluator.Truth.TRUE,
                        PredicateEvaluator.Truth.TRUE)
                == PredicateEvaluator.Truth.TRUE,
                "TRUE AND TRUE");

        require(
                PredicateEvaluator.and(
                        PredicateEvaluator.Truth.TRUE,
                        PredicateEvaluator.Truth.UNKNOWN)
                == PredicateEvaluator.Truth.UNKNOWN,
                "TRUE AND UNKNOWN");

        require(
                PredicateEvaluator.and(
                        PredicateEvaluator.Truth.FALSE,
                        PredicateEvaluator.Truth.UNKNOWN)
                == PredicateEvaluator.Truth.FALSE,
                "FALSE AND UNKNOWN");

        require(
                PredicateEvaluator.and(
                        PredicateEvaluator.Truth.UNKNOWN,
                        PredicateEvaluator.Truth.UNKNOWN)
                == PredicateEvaluator.Truth.UNKNOWN,
                "UNKNOWN AND UNKNOWN");
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
