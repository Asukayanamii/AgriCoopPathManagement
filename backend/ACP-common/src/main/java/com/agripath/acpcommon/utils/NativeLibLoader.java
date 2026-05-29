package com.agripath.acpcommon.utils;

import java.io.*;
import java.nio.file.*;

public class NativeLibLoader {
    private static boolean loaded = false;

    public static synchronized void loadLibraries() {
        if (loaded) return;
        try {
            String tmpDir = System.getProperty("java.io.tmpdir");
            Path nativeDir = Paths.get(tmpDir, "agripath_native");
            Files.createDirectories(nativeDir);

            String[] libs = {"k_means_cluster.dll", "ResourceSearchJNI.dll", "AStarJNI.dll"};
            for (String lib : libs) {
                Path dest = nativeDir.resolve(lib);
                if (!Files.exists(dest)) {
                    try (InputStream is = NativeLibLoader.class.getResourceAsStream("/native/" + lib)) {
                        if (is != null) {
                            Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
                        }
                    }
                }
                if (Files.exists(dest)) {
                    System.load(dest.toAbsolutePath().toString());
                }
            }
            loaded = true;
        } catch (Exception e) {
            System.err.println("Failed to load native libraries: " + e.getMessage());
        }
    }
}
