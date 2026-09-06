import java.util.*;

public final class ExecutionModelTest {

    public static void main(String[] args) {
        testRowPreservesOrderAndNull();
        testRelationDataAcceptsMatchingWidth();
        testRelationDataRejectsWrongWidth();
        testRelationDataRejectsNullRow();

        System.out.println(
                "ExecutionModelTest PASSED");
    }

    private static void testRowPreservesOrderAndNull() {
        ExecutionModel.Row row =
                new ExecutionModel.Row(
                        Arrays.asList(
                                "A",
                                null,
                                7L));

        require(
                row.size() == 3,
                "row width");

        require(
                row.get(0).equals("A"),
                "row first value");

        require(
                row.get(1) == null,
                "row semantic null");

        require(
                row.get(2).equals(7L),
                "row third value");
    }

    private static void testRelationDataAcceptsMatchingWidth() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false),
                                new SemanticModel.Column(
                                        "amount",
                                        SemanticModel.Type.DECIMAL,
                                        true)));

        ExecutionModel.RelationData data =
                new ExecutionModel.RelationData(
                        schema,
                        List.of(
                                new ExecutionModel.Row(
                                        Arrays.asList(
                                                "A",
                                                null))));

        require(
                data.schema == schema,
                "relation schema identity");

        require(
                data.rows.size() == 1,
                "relation row count");

        require(
                data.rows.get(0).get(1) == null,
                "relation semantic null");
    }

    private static void testRelationDataRejectsWrongWidth() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false)));

        try {
            new ExecutionModel.RelationData(
                    schema,
                    List.of(
                            new ExecutionModel.Row(
                                    List.of(
                                            "A",
                                            "extra"))));

            throw new AssertionError(
                    "Expected row-width failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "does not match schema width"),
                    "row-width failure message");
        }
    }

    private static void testRelationDataRejectsNullRow() {
        SemanticModel.Schema schema =
                new SemanticModel.Schema(
                        List.of(
                                new SemanticModel.Column(
                                        "category",
                                        SemanticModel.Type.TEXT,
                                        false)));

        List<ExecutionModel.Row> rows =
                new ArrayList<>();

        rows.add(null);

        try {
            new ExecutionModel.RelationData(
                    schema,
                    rows);

            throw new AssertionError(
                    "Expected null-row failure");

        } catch (IllegalArgumentException ex) {
            require(
                    ex.getMessage() != null &&
                    ex.getMessage().contains(
                            "null row"),
                    "null-row failure message");
        }
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
