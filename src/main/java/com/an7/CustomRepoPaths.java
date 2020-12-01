package com.an7;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CustomRepoPaths {
    private static final Map<String, String> paths;

    static {
        Map<String, String> mutablePaths = new HashMap<>();
        mutablePaths.put("libasynql", "libasynql/libasynql");
        mutablePaths.put("await-generator", "await-generator/await-generator");
        paths = Collections.unmodifiableMap(mutablePaths);
    }

    public static Map<String, String> get() {
        return paths;
    }
}
