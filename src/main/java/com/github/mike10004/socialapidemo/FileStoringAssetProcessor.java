package com.github.mike10004.socialapidemo;

import com.google.common.base.CharMatcher;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.Files;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static com.google.common.base.Preconditions.checkNotNull;

public class FileStoringAssetProcessor implements AssetProcessor {

    private static final CharMatcher safeChars = CharMatcher.javaLetterOrDigit().or(CharMatcher.is('-'));
    private static final CharMatcher unsafeChars = safeChars.negate();

    private static final Logger log = LoggerFactory.getLogger(FileStoringAssetProcessor.class);

    private Path root;
    private AssetSerializer serializer;

    public FileStoringAssetProcessor(Path root, AssetSerializer serializer) {
        this.root = checkNotNull(root);
        this.serializer = checkNotNull(serializer);
    }

    @Override
    public void process(Object asset, Iterable<String> lineage) {
        if (asset == null) {
            log.info("skipping null asset");
            return;
        }
        Path target = resolvePath(lineage);
        try {
            File outputDir = target.toFile();
            if (!outputDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                outputDir.mkdirs();
            }
            if (!outputDir.isDirectory()) {
                throw new IOException("directory absent and creation failed: " + outputDir);
            }
            File outputFile = File.createTempFile(createFilenamePrefix(asset), createFilenameSuffix(asset), outputDir);
            ByteSource serialized = serializer.serialize(asset);
            if (serialized == null) {
                log.debug("skipping unserializable data");
                return;
            }
            serialized.copyTo(Files.asByteSink(outputFile));
        } catch (IOException e) {
            log.warn("failed to store asset", e);
        }
    }

    public interface AssetSerializer {

        @Nullable
        ByteSource serialize(Object asset);

        @Nullable
        default ByteSource serialize(String data) {
            return serialize(CharSource.wrap(data));
        }

        @Nullable
        default ByteSource serialize(CharSource charSource) {
            return charSource.asByteSource(StandardCharsets.UTF_8);
        }

    }

    protected String createFilenamePrefix(Object asset) {
        return "asset";
    }

    protected String createFilenameSuffix(Object asset) {
        return "";
    }

    protected Path resolvePath(Iterable<String> lineage) {
        Path target = root;
        for (String segment : lineage) {
            if (segment != null) {
                target = target.resolve(makeSafe(segment));
            }
        }
        return target;
    }

    protected String makeSafe(String pathComponent) {
        if (pathComponent.isEmpty()) {
            pathComponent = "_";
        }
        pathComponent = unsafeChars.replaceFrom(pathComponent, '_');
        return pathComponent;
    }
}
