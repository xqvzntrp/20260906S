import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NegativeCapsuleTest {
    private static final Path BASE =
            Path.of("enterprise-cloud-gtm-capsule-v9");

    private static final Path FIXTURE_ROOT =
            Path.of("out", "negative-capsules");

    private record Case(
            String name,
            String fixture,
            Mode mode,
            String expectedFragment) {}

    private enum Mode {
        VALIDATE,
        RUN
    }

    private record Result(int exitCode, String output) {}

    public static void main(String[] args) throws Exception {
        require(
                Files.isDirectory(BASE),
                "missing baseline capsule: " + BASE);

        recreateFixtureRoot();

        List<Case> cases = List.of(
                new Case(
                        "unknown relation input",
                        "unknown-relation-input",
                        Mode.VALIDATE,
                        "unknown source"),
                new Case(
                        "bad output step",
                        "bad-output-step",
                        Mode.VALIDATE,
                        "output"),
                new Case(
                        "ambiguous left join",
                        "ambiguous-left-join",
                        Mode.VALIDATE,
                        "ambiguous non-key output column"),
                new Case(
                        "unknown expression function",
                        "unknown-expression-function",
                        Mode.VALIDATE,
                        "function"),
                new Case(
                        "invalid predicate operator",
                        "invalid-predicate-operator",
                        Mode.VALIDATE,
                        "operator"),
                new Case(
                        "divide by zero",
                        "divide-by-zero",
                        Mode.RUN,
                        "zero"),
                new Case(
                        "invalid numeric declaration",
                        "invalid-numeric-declaration",
                        Mode.VALIDATE,
                        "precision"),
                new Case(
                        "invalid expected numeric declaration",
                        "invalid-expected-numeric-declaration",
                        Mode.VALIDATE,
                        "precision"),
                new Case(
                        "verification mismatch",
                        "verification-mismatch",
                        Mode.RUN,
                        "verification failed"));

        prepareUnknownRelationInput();
        prepareBadOutputStep();
        prepareAmbiguousLeftJoin();
        prepareUnknownExpressionFunction();
        prepareInvalidPredicateOperator();
        prepareDivideByZero();
        prepareInvalidNumericDeclaration();
        prepareInvalidExpectedNumericDeclaration();
        prepareVerificationMismatch();

        int assertions = 0;

        for (Case c : cases) {
            Result result = invoke(c);

            require(
                    result.exitCode != 0
                            || result.output.contains("INVALID")
                            || result.output.contains("FAILED"),
                    c.name
                            + ": expected failure but command appeared successful\n"
                            + result.output);

            require(
                    result.output.toLowerCase()
                            .contains(c.expectedFragment.toLowerCase()),
                    c.name
                            + ": expected diagnostic fragment <"
                            + c.expectedFragment
                            + "> but output was:\n"
                            + result.output);

            assertions += 2;

            System.out.println(
                    "PASS: "
                            + c.name
                            + " [exit="
                            + result.exitCode
                            + "]");
        }

        System.out.println(
                "NegativeCapsuleTest PASSED: "
                        + assertions
                        + " assertions across "
                        + cases.size()
                        + " negative cases");
    }

    private static void prepareUnknownRelationInput() throws IOException {
        Path root = freshCopy("unknown-relation-input");
        Path path = root.resolve("relations/partner_readiness_gaps.json");

        replaceRequired(
                path,
                "\"readiness\": \"partner_product_readiness\"",
                "\"readiness\": \"relation_that_does_not_exist\"");
    }

    private static void prepareBadOutputStep() throws IOException {
        Path root = freshCopy("bad-output-step");
        Path path =
                root.resolve(
                        "relations/marketplace_product_economics_components.json");

        replaceRequired(
                path,
                "\"output\": \"economics\"",
                "\"output\": \"step_that_does_not_exist\"");
    }

    private static void prepareAmbiguousLeftJoin() throws IOException {
        Path root = freshCopy("ambiguous-left-join");
        Path path =
                root.resolve("relations/partner_product_readiness.json");

        String text = Files.readString(path);

        int projectionStart =
                text.indexOf(
                        "    {\n"
                                + "      \"id\": \"expertise_projection\"");

        int authStart =
                text.indexOf(
                        "    {\n"
                                + "      \"id\": \"with_authorization\"");

        require(
                projectionStart >= 0 && authStart > projectionStart,
                "could not locate expertise projection block");

        text =
                text.substring(0, projectionStart)
                        + text.substring(authStart);

        text =
                replaceRequired(
                        text,
                        "\"left\": \"expertise_projection\"",
                        "\"left\": \"with_expertise\"");

        Files.writeString(path, text);
    }

    private static void prepareUnknownExpressionFunction()
            throws IOException {

        Path root = freshCopy("unknown-expression-function");
        Path path =
                root.resolve(
                        "relations/marketplace_product_economics.json");

        replaceRequired(
                path,
                "\"function\": \"subtract\"",
                "\"function\": \"definitely_not_a_function\"");
    }

    private static void prepareInvalidPredicateOperator()
            throws IOException {

        Path root = freshCopy("invalid-predicate-operator");
        Path path =
                root.resolve("relations/partner_readiness_gaps.json");

        replaceRequired(
                path,
                "\"operator\": \"!=\"",
                "\"operator\": \"CONTAINS_BUT_NOT_SUPPORTED\"");
    }

    private static void prepareDivideByZero() throws IOException {
        Path root = freshCopy("divide-by-zero");
        Path path =
                root.resolve(
                        "relations/marketplace_product_economics.json");

        String text = Files.readString(path);

        int margin =
                text.indexOf("\"name\": \"margin_ratio\"");

        require(margin >= 0, "could not locate margin_ratio");

        int divide =
                text.indexOf(
                        "\"function\": \"divide\"",
                        margin);

        require(divide >= 0, "could not locate margin divide");

        int args =
                text.indexOf("\"args\"", divide);

        require(args >= 0, "could not locate divide args");

        int arrayStart =
                text.indexOf('[', args);

        require(arrayStart >= 0, "could not locate divide args array");

        int firstObjectStart =
                text.indexOf('{', arrayStart);

        require(
                firstObjectStart >= 0,
                "could not locate first divide argument");

        int firstObjectEnd =
                text.indexOf('}', firstObjectStart);

        require(
                firstObjectEnd >= 0,
                "could not locate first divide argument end");

        int secondObjectStart =
                text.indexOf('{', firstObjectEnd + 1);

        require(
                secondObjectStart >= 0,
                "could not locate margin denominator");

        int secondObjectEnd =
                text.indexOf('}', secondObjectStart);

        require(
                secondObjectEnd >= 0,
                "could not locate margin denominator end");

        String replacement =
                "{\n"
                        + "            \"value\": 0\n"
                        + "          }";

        text =
                text.substring(0, secondObjectStart)
                        + replacement
                        + text.substring(secondObjectEnd + 1);

        Files.writeString(path, text);
    }

    private static void prepareInvalidNumericDeclaration()
            throws IOException {

        Path root = freshCopy("invalid-numeric-declaration");
        Path path =
                root.resolve(
                        "relations/marketplace_product_economics.json");

        String text = Files.readString(path);

        int margin =
                text.indexOf("\"name\": \"margin_ratio\"");

        require(margin >= 0, "could not locate margin_ratio");

        int expression =
                text.indexOf("\"expression\":", margin);

        require(expression >= 0, "could not locate margin expression");

        String insertion =
                "\"precision\": 39,\n"
                        + "          \"scale\": 8,\n"
                        + "          ";

        text =
                text.substring(0, expression)
                        + insertion
                        + text.substring(expression);

        Files.writeString(path, text);
    }

    private static void prepareInvalidExpectedNumericDeclaration()
            throws IOException {

        Path root =
                freshCopy("invalid-expected-numeric-declaration");

        Path path =
                root.resolve(
                        "expected/marketplace_product_economics.schema.json");

        String text = Files.readString(path);

        java.util.regex.Pattern numberType =
                java.util.regex.Pattern.compile(
                        "\"type\"\\s*:\\s*\"NUMBER\"");

        java.util.regex.Matcher matcher =
                numberType.matcher(text);

        require(
                matcher.find(),
                "could not locate NUMBER column in expected schema");

        int type = matcher.start();

        int objectStart =
                text.lastIndexOf('{', type);

        int objectEnd =
                text.indexOf('}', type);

        require(
                objectStart >= 0 && objectEnd > objectStart,
                "could not locate expected NUMBER column object");

        String column =
                text.substring(
                        objectStart,
                        objectEnd + 1);

        java.util.regex.Pattern precisionPattern =
                java.util.regex.Pattern.compile(
                        "\"precision\"\\s*:\\s*-?\\d+");

        java.util.regex.Matcher precisionMatcher =
                precisionPattern.matcher(column);

        if (precisionMatcher.find()) {
            column =
                    precisionMatcher.replaceFirst(
                            "\"precision\":39");
        } else {
            int insertAt = matcher.end() - objectStart;

            column =
                    column.substring(0, insertAt)
                            + ",\"precision\":39"
                            + column.substring(insertAt);
        }

        text =
                text.substring(0, objectStart)
                        + column
                        + text.substring(objectEnd + 1);

        Files.writeString(path, text);
    }

    private static void prepareVerificationMismatch()
            throws IOException {

        Path root = freshCopy("verification-mismatch");
        Path path =
                root.resolve("expected/product_bookings.csv");

        List<String> lines =
                new ArrayList<>(Files.readAllLines(path));

        require(
                lines.size() >= 2,
                "product_bookings expected fixture unexpectedly empty");

        String[] parts = lines.get(1).split(",", -1);

        boolean changed = false;

        for (int i = 1; i < parts.length; i++) {
            try {
                Double.parseDouble(parts[i]);
                parts[i] = "999999999.00";
                changed = true;
                break;
            } catch (NumberFormatException ignored) {
            }
        }

        require(
                changed,
                "could not find numeric field in product_bookings");

        lines.set(1, String.join(",", parts));

        Files.write(
                path,
                lines,
                StandardCharsets.UTF_8);
    }

    private static Path freshCopy(String name)
            throws IOException {

        Path target = FIXTURE_ROOT.resolve(name);

        copyTree(BASE, target);

        Path generated = target.resolve("generated");

        if (Files.exists(generated)) {
            deleteTree(generated);
        }

        Files.createDirectories(generated);

        return target;
    }

    private static void recreateFixtureRoot()
            throws IOException {

        if (Files.exists(FIXTURE_ROOT)) {
            deleteTree(FIXTURE_ROOT);
        }

        Files.createDirectories(FIXTURE_ROOT);
    }

    private static void copyTree(
            Path source,
            Path target)
            throws IOException {

        try (var stream = Files.walk(source)) {
            for (Path src : stream.toList()) {
                Path relative = source.relativize(src);
                Path dst = target.resolve(relative);

                if (Files.isDirectory(src)) {
                    Files.createDirectories(dst);
                } else {
                    Files.createDirectories(dst.getParent());
                    Files.copy(
                            src,
                            dst,
                            StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private static void deleteTree(Path root)
            throws IOException {

        try (var stream = Files.walk(root)) {
            for (Path path :
                    stream.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }

    private static void replaceRequired(
            Path path,
            String oldText,
            String newText)
            throws IOException {

        String text = Files.readString(path);

        text =
                replaceRequired(
                        text,
                        oldText,
                        newText);

        Files.writeString(path, text);
    }

    private static String replaceRequired(
            String text,
            String oldText,
            String newText) {

        require(
                text.contains(oldText),
                "expected text not found: " + oldText);

        return text.replace(oldText, newText);
    }

    private static Result invoke(Case c)
            throws IOException, InterruptedException {

        Path fixture =
                FIXTURE_ROOT.resolve(c.fixture);

        String command =
                c.mode == Mode.VALIDATE
                        ? "ValidateCapsule"
                        : "RunCapsule";

        ProcessBuilder builder =
                new ProcessBuilder(
                        javaCommand(),
                        "-cp",
                        "out",
                        command,
                        fixture.toString());

        builder.redirectErrorStream(true);

        Process process = builder.start();

        String output =
                new String(
                        process.getInputStream().readAllBytes(),
                        StandardCharsets.UTF_8);

        int exitCode = process.waitFor();

        return new Result(exitCode, output);
    }

    private static String javaCommand() {
        return Path.of(
                        System.getProperty("java.home"),
                        "bin",
                        "java")
                .toString();
    }

    private static void require(
            boolean condition,
            String message) {

        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
