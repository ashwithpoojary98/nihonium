package io.github.ashwith.chrome;

import lombok.Data;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class ChromeOption {
    private String binary;
    private final List<String> args = new ArrayList<>();
    private final List<File> extensionFiles = new ArrayList<>();
    private final List<String> extensions = new ArrayList<>();
    private final Map<String, Object> experimentalOptions = new HashMap<>();

    public void addArgument(String arg) {
        this.args.add(arg);
    }

    public void addExtensionFile(File extensionFile) {
        this.extensionFiles.add(extensionFile);
    }

    public void addExtension(String extension) {
        this.extensions.add(extension);
    }

}
