package com.example.agentlearn.web;

import com.example.agentlearn.TestSupport;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ManagedFileServiceTest {
    public static void main(String[] args) throws Exception {
        listsReadsWritesAndDeletesManagedFiles();
        rejectsTraversalPaths();
    }

    private static void listsReadsWritesAndDeletesManagedFiles() throws Exception {
        Path root = Files.createTempDirectory("managed-files");
        ManagedFileService service = new ManagedFileService(root, List.of(".md", ".txt"));

        service.write("notes/agent.md", "# Agent");
        service.write("plain.txt", "hello");

        List<ManagedFile> files = service.list();

        TestSupport.assertEquals(files.size(), 2);
        TestSupport.assertEquals(service.read("notes/agent.md"), "# Agent");
        TestSupport.assertTrue(files.stream().anyMatch(file -> file.path().equals("notes/agent.md")), "Expected nested markdown file in listing");

        service.delete("plain.txt");

        TestSupport.assertEquals(service.list().size(), 1);
    }

    private static void rejectsTraversalPaths() throws Exception {
        Path root = Files.createTempDirectory("managed-files");
        ManagedFileService service = new ManagedFileService(root, List.of(".md"));

        boolean rejected = false;
        try {
            service.write("../escape.md", "bad");
        } catch (IllegalArgumentException e) {
            rejected = true;
        }

        TestSupport.assertTrue(rejected, "Expected traversal path to be rejected");
    }
}
