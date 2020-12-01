package com.an7;

import com.beust.jcommander.JCommander;

public class Main {


    public static void main(String[] args) {
        Args commandArgs = new Args();
        JCommander.newBuilder()
                .addObject(commandArgs)
                .build()
                .parse(args);
        Processor p = new Processor(commandArgs);
        p.run();
    }
}
