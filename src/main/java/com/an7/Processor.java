package com.an7;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Processor {

    private final Args args;

    private final Map<String, Path> repoFiles;
    private final List<Path> processablePaths;
    private final List<Path> entries;

    public Processor(Args args) {
        this.args = args;

        // Initialize from args
        try {
            // Dictionary of repo entries
            repoFiles = getRepoFiles(args.getRepoSource());

            // Processable paths:
            // Case 1: traditional pmmp, 3 things: the pmmp path, the /plugins path, and /virions path.
            // Case 2: docker pmmp, 2 things: /plugins, and /data/virions
            processablePaths = getProcessablePaths(args.getDirectories());

            // Entries: Relinkable paths, including PocketMine-MP.phar, *.phar and folders in /plugins and /virions
            entries = getEntries(processablePaths);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }

    public void run() {
        try {
            // Backing up all things
            for (Path processablePath : processablePaths) {
                 if (args.isBackup()) {
                    if (Helper.isPmmpPath(processablePath)) { // backup the pmmp phar if is pmmp path
                        backupFile(processablePath.resolve(Helper.PMMP_FILE));
                    } else { // backup the folder if is /plugins or /virions
                        backupFolder(processablePath);
                    }
                }
            }

            // Start relinking
            processEntries(entries);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
        System.out.println("Done!");
    }

    private Map<String, Path> getRepoFiles(String repoDir) throws IOException {
        Path repoPath = Paths.get(repoDir);
        if (!Files.isDirectory(repoPath)) {
            throw new RuntimeException("Repo not exists: " + repoDir);
        }
        return Files.walk(repoPath, 1, FileVisitOption.FOLLOW_LINKS)
                .collect(Collectors.toMap(path -> path.getFileName().toString(), path -> {
                    // Custom repo paths (for plugins that don't have /src folder in root)
                    Map<String, String> customPaths = CustomRepoPaths.get();

                    Path finalPath;
                    if (Files.notExists(path.resolve("src")) && customPaths.containsKey(path.getFileName().toString())) {
                        finalPath = path.getParent().resolve(customPaths.get(path.getFileName().toString()));
                        if (Files.notExists(finalPath)) {
                            throw new RuntimeException("Can't resolve custom path for repo: " + finalPath.toString());
                        }
                    } else {
                        finalPath = path;
                    }
                    return finalPath.toAbsolutePath();
                }));
    }

    private List<Path> getProcessablePaths(List<String> directories) {
        List<Path> processablePaths = new ArrayList<>();
        for (String dir : directories) {
            Path dirPath = Paths.get(dir);
            if (!Files.isDirectory(dirPath)) {
                System.out.println("Directory not exists: " + dir + ", skipping...");
                continue;
            }
            dirPath = dirPath.toAbsolutePath(); // Fix workspace errors (getParent() == null)

            // See if have /plugins or /virions paths. Add those.
            Path pmmpPhar = dirPath.resolve("PocketMine-MP.phar");
            if (Files.isRegularFile(pmmpPhar)) {
                System.out.println("Detected traditional pmmp folder: " + dirPath + ". Adding PocketMine-MP.phar, /plugins and /virions to process...");
                processablePaths.add(pmmpPhar);
                Path pluginsPath = dirPath.resolve("plugins");
                Path virionsPath = dirPath.resolve("virions");
                if (Files.isDirectory(pluginsPath)) {
                    processablePaths.add(pluginsPath);
                }
                if (Files.isDirectory(virionsPath)) {
                    processablePaths.add(virionsPath);
                }
            } else {
                Path pluginsPath = dirPath.resolve("plugins");
                Path virionsPath = dirPath.resolve("data").resolve("virions");
                if (Files.isDirectory(pluginsPath) && Files.isDirectory(virionsPath)) {
                    System.out.println("Detected Docker pmmp folder: " + dirPath + ". Adding /plugins and /data/virions to process...");
                    processablePaths.add(pluginsPath);
                    processablePaths.add(virionsPath);
                }
            }
        }
        return processablePaths;
    }

    private List<Path> getEntries(List<Path> processablePaths) throws IOException {
        List<Path> entries = new ArrayList<>();
        for (Path dirPath : processablePaths) {
            if (dirPath.getFileName().toString().equals("PocketMine-MP.phar")
                && (Files.isRegularFile(dirPath) || Files.isSymbolicLink(dirPath))) {
                System.out.println("PocketMine-MP.phar detected in " + dirPath + "! Adding to be linked...");
                entries.add(dirPath);
            } else {
                // This is /plugins or /virions folder. Add all.
                entries.addAll(getEntries(dirPath));
            }
        }
        return entries;
    }

    private List<Path> getEntries(Path pluginsPath) throws IOException {
        return Files.walk(pluginsPath, 1).skip(1).collect(Collectors.toList());
    }

    private void backupFolder(Path folder) throws IOException {
        if (!Files.isDirectory(folder)) {
            return;
        }
        Path relinkFolderPath = folder.getParent()
                .resolve("relink-" + folder.getFileName() + "-" + Instant.now().getEpochSecond());
        // no pure java here?
        FileUtils.copyDirectory(new File(folder.toString()), new File(relinkFolderPath.toString()));
    }

    private void backupFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        String relinkFilePath = file.getParent()
                .resolve(FilenameUtils.getBaseName(file.getFileName().toString())
                        + "-"
                        + Instant.now().getEpochSecond()).toString()
                        + "." + FilenameUtils.getExtension(file.getFileName().toString());
        // no pure java here?
        FileUtils.copyFile(new File(file.toString()), new File(relinkFilePath));
    }

    private void processEntries(List<Path> entries) {
        entries.forEach(entry -> {
            // Find in repo...
            String pharName = FilenameUtils.getBaseName(entry.getFileName().toString()) + ".phar";
            String folderName = FilenameUtils.getBaseName(entry.getFileName().toString());
            try {
                if (repoFiles.containsKey(pharName)) { // Check if phar exists
                    Files.deleteIfExists(entry.getParent().resolve(pharName));
                    Files.createSymbolicLink(entry.getParent().resolve(pharName),
                            entry.getParent().relativize(repoFiles.get(pharName)));
                } else if (repoFiles.containsKey(folderName)) { // Check if folder exists
                    FileUtils.deleteDirectory(entry.getParent().resolve(folderName).toFile());
                    Files.createSymbolicLink(entry.getParent().resolve(folderName),
                            entry.getParent().relativize(repoFiles.get(folderName)));
                } else { // Can't find in repo
                    System.out.println("Can't find " + entry.getFileName() + " in repo. Skipping...");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
