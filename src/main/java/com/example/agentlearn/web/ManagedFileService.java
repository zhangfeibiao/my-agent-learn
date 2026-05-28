package com.example.agentlearn.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ManagedFileService {
    private final Path root;
    private final List<String> allowedExtensions;

    public ManagedFileService(Path root, List<String> allowedExtensions) {
        this.root = root.toAbsolutePath().normalize();
        this.allowedExtensions = allowedExtensions.stream()
                .map(extension -> extension.toLowerCase(Locale.ROOT))
                .toList();
    }

    public List<ManagedFile> list() throws IOException {
        if (!Files.exists(root)) {
            return List.of();
        }

        List<ManagedFile> files = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(this::hasAllowedExtension)
                    .forEach(path -> {
                        try {
                            files.add(new ManagedFile(relativePath(path), Files.size(path)));
                        } catch (IOException e) {
                            throw new ManagedFileException(e);
                        }
                    });
        } catch (ManagedFileException e) {
            throw (IOException) e.getCause();
        }

        files.sort(Comparator.comparing(ManagedFile::path));
        return files;
    }

    public String read(String relativePath) throws IOException {
        Path file = resolveAllowedFile(relativePath);
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    public void write(String relativePath, String content) throws IOException {
        Path file = resolveAllowedFile(relativePath);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }

    public void delete(String relativePath) throws IOException {
        Path file = resolveAllowedFile(relativePath);
        Files.deleteIfExists(file);
    }

    private Path resolveAllowedFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new IllegalArgumentException("Path is required");
        }
        Path resolved = root.resolve(relativePath).normalize().toAbsolutePath();
        if (!resolved.startsWith(root)) {
            throw new IllegalArgumentException("Path escapes managed directory");
        }
        if (!hasAllowedExtension(resolved)) {
            throw new IllegalArgumentException("Unsupported file extension");
        }
        return resolved;
    }

    private boolean hasAllowedExtension(Path path) {
        String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return allowedExtensions.stream().anyMatch(fileName::endsWith);
    }

    private String relativePath(Path path) {
        return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private static final class ManagedFileException extends RuntimeException {
        private ManagedFileException(Throwable cause) {
            super(cause);
        }
    }
}
