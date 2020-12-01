package com.an7;

import java.nio.file.Files;
import java.nio.file.Path;

public class Helper {
    public static final String PMMP_FILE = "PocketMine-MP.phar";

    public static boolean isPmmpPath(Path path) {
        Path pmmpPhar = path.resolve(PMMP_FILE);
        return Files.isRegularFile(pmmpPhar);
    }
}
