package ca.ubc.ece.resess.slicer.dynamic.slicer4j;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import ca.ubc.ece.resess.slicer.dynamic.core.exceptions.InvalidCommandsException;

public class CommandParser {
    public static final String CMD_LINE_SYNTAX = "java -jar Slicer4j/target/slicer4j-jar-with-dependencies.jar";
    static Options options;
    static {
        options = new Options();
        options.addOption("h", "help", false, "Display this help and exit");
        options.addOption("m", "mode", true, "Tool mode: i for instrument, g to produce the dynamic control flow graph, s to slice");
        options.addOption("j", "jar", true, "jar path");
        options.addOption("lc", "logger-classes", true, "logger classes jar");
        options.addOption("t", "trace", true, "Execution trace"); // fileToParse
        options.addOption("sp", "slice-position", true, "starting statement for the slice"); // posToSlice
        options.addOption("sv", "slice-variables", true, "dash-joined list of starting variables to slice from"); // variables
        options.addOption("fw", "forward-slice-position", true, "starting statement for the forward slice (for chopping)"); 
        options.addOption("sl", "static-log", true, "Static-log file path"); // static-log file
        options.addOption("o", "outDir", true, "Output directory"); // staticLogFile
        options.addOption("sd", "stub-droid", true, "Location of the StubDroid summaries"); // stubDroidPath
        options.addOption("tw", "taint-wrapper", true, "Location of the FlowDroid's taint-wrapper list"); // taintWrapperPath
        options.addOption("data", "data-only", false, "Track data-flow dependence only"); // data
        options.addOption("ctrl", "Control-only", false, "Track control dependence only"); // data
        options.addOption("f", "framework", true, "Path to folder with extra framework methods"); // path of extra framework methods
        options.addOption("d", "debug", false, "Enable debug"); // data
        options.addOption("once", "once", false, "Slice one step only"); // data
    }


    private CommandParser() {
        throw new IllegalStateException("Utility class");
    }

    public static Map<String, String> parse(String[] args){
        Map<String, String> parsed = new HashMap<>();
        try {
            CommandLine cmd = (new DefaultParser()).parse(options, args);
            if (cmd.hasOption("h")) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp(CMD_LINE_SYNTAX, options);
                return parsed;
            }
            String[] optionTags = {"m", "j", "p", "c", "lc", "t", "sp", "sv", "o", "sd", "tw", "sl", "fw", "f"};
            for (String opt: Arrays.asList(optionTags)) {
                String value = cmd.getOptionValue(opt);
                if (value != null) {
                    parsed.put(opt, value);
                }
            }
            if (cmd.hasOption("data")) {
                parsed.put("data", "True");
            }
            if (cmd.hasOption("ctrl")) {
                parsed.put("ctrl", "True");
            }
            if (cmd.hasOption("once")) {
                parsed.put("once", "True");
            }
            if (cmd.hasOption("d")) {
                parsed.put("debug", "True");
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(CMD_LINE_SYNTAX, options);
            throw new InvalidCommandsException(e);
        }
        return parsed;
    }

    public static Options getOptions() {
        return options;
    }
}
