package com.an7;

import com.beust.jcommander.Parameter;

import java.util.List;

public class Args {

    @Parameter(names = {"--source", "-s"}, required = true)
    public String repoSource;

    @Parameter(description = "/plugins folder paths", required = true)
    public List<String> directories;

    @Parameter(names = "--backup", description = "Backup /plugins folder to /relink<timestamp> folder", arity = 1)
    public boolean backup = true;

    public String getRepoSource() {
        return repoSource;
    }

    public List<String> getDirectories() {
        return directories;
    }

    public boolean isBackup() {
        return backup;
    }
}
