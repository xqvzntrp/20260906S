import java.nio.file.Path;

public interface CapsuleParser {

    RawModel.Capsule parse(Path capsuleRoot);

    RawModel.RelationDocument relation(
            RawModel.Relation declaration,
            Path capsuleRoot);

    RawSchema schema(
            String relativePath,
            Path capsuleRoot);
}