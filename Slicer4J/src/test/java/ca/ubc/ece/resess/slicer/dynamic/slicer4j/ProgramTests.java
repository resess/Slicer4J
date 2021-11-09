package ca.ubc.ece.resess.slicer.dynamic.slicer4j;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;


public class ProgramTests {

    Path root = Paths.get(".").normalize().toAbsolutePath();
    Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    Path outDir = Paths.get(slicerPath.toString(), "testTempDir");
    Path sliceLogger = Paths.get(root.getParent().getParent().toString(), "DynamicSlicingCore" + File.separator + "DynamicSlicingLoggingClasses" + File.separator + "DynamicSlicingLogger.jar");

    @BeforeAll 
    static void preCleanUp() throws IOException {
        TestUtils.cleanWorkingDirectory();
    }
    
    @AfterEach 
    void postCleanUp() throws IOException {
        TestUtils.cleanWorkingDirectory();
    }


    @Test
    void issue1() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue1");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test1-issue-1.0.0.jar").toString();
        TestUtils.buildJar(testPath);
        
        String [] args = {
            "-m", 
            "i",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-lc",
            sliceLogger.toString()
        };
        Slicer.main(args);
        
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "java -Xmx8g -cp " + outDir.toString() + File.separator + "test1-issue-1.0.0_i.jar Main | grep \"SLICING\"");
        System.out.println(pb.command());
        Process p = pb.start();
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outDir.toString() + File.separator + "trace.log"));
        String readline;
        while ((readline = reader.readLine()) != null) {
            writer.write(readline);
            writer.write("\n");
        }
        writer.close();
        reader.close();
        
        args = new String [] {
            "-m", 
            "g",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-t",
            outDir.toString() + File.separator + "trace.log",
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-sd",
            root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual",
            "-tw",
            root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt"
        };
        Slicer.main(args);
        
        args = new String [] {
            "-m", 
            "s",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-t",
            outDir.toString() + File.separator + "trace.log",
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-sd",
            root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual",
            "-tw",
            root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt",
            "-sp",
            "15"
        };
        Slicer.main(args);
        
        Path outputPath = Paths.get(slicerPath.toString(), "testTempDir" + File.separator + "slice.log");
        List<String> out = Files.readAllLines(outputPath);
        System.out.println(out);
        
        assertEquals(Arrays.asList(
        "Main:6",
        "Main:7",
        "Main:17",
        "Main:18",
        "Main:8",
        "Main:13",
        "Main:9"), 
        out);
    }
    
    @Test
    void issue2() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue2");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test2-issue-1.0.0.jar").toString();
        TestUtils.buildJar(testPath);
        
        String [] args = {
            "-m", 
            "i",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-lc",
            sliceLogger.toString()
        };
        Slicer.main(args);
        
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "java -Xmx8g -cp " + outDir.toString() + File.separator + "test2-issue-1.0.0_i.jar Main something | grep \"SLICING\"");
        System.out.println(pb.command());
        Process p = pb.start();
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outDir.toString() + File.separator + "trace.log"));
        String readline;
        while ((readline = reader.readLine()) != null) {
            writer.write(readline);
            writer.write("\n");
        }
        writer.close();
        reader.close();
        
        args = new String [] {
            "-m", 
            "g",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-t",
            outDir.toString() + File.separator + "trace.log",
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-sd",
            root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual",
            "-tw",
            root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt"
        };
        Slicer.main(args);
        
        args = new String [] {
            "-m", 
            "s",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-t",
            outDir.toString() + File.separator + "trace.log",
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-sd",
            root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual",
            "-tw",
            root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt",
            "-sp",
            "16"
        };
        Slicer.main(args);
        
        Path outputPath = Paths.get(slicerPath.toString(), "testTempDir" + File.separator + "slice.log");
        List<String> out = Files.readAllLines(outputPath);
        System.out.println(out);
        
        assertEquals(Arrays.asList(
        "Main:6",
        "Main:15",
        "Main:16",
        "Main:19",
        "Main:8",
        "Main:9",
        "Main:11"), 
        out);
    }
    
    
    @Test
    void sliceOnce() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue1");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test1-issue-1.0.0.jar").toString();
        TestUtils.buildJar(testPath);
        
        String [] args = {
            "-m", 
            "i",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-lc",
            sliceLogger.toString()
        };
        Slicer.main(args);
        
        ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", "java -Xmx8g -cp " + outDir.toString() + File.separator + "test1-issue-1.0.0_i.jar Main | grep \"SLICING\"");
        System.out.println(pb.command());
        Process p = pb.start();
        p.waitFor();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        BufferedWriter writer = Files.newBufferedWriter(Paths.get(outDir.toString() + File.separator + "trace.log"));
        String readline;
        while ((readline = reader.readLine()) != null) {
            writer.write(readline);
            writer.write("\n");
        }
        writer.close();
        reader.close();
        
        args = new String [] {
            "-m", 
            "g",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-t",
            outDir.toString() + File.separator + "trace.log",
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-sd",
            root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual",
            "-tw",
            root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt"
        };
        Slicer.main(args);
        
        args = new String [] {
            "-m", 
            "s",
            "-j",
            jarPath,
            "-o",
            outDir.toString(), 
            "-t",
            outDir.toString() + File.separator + "trace.log",
            "-sl", 
            outDir.toString() + File.separator + "static_log.log",
            "-sd",
            root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual",
            "-tw",
            root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt",
            "-sp",
            "15",
            "-once"
        };
        Slicer.main(args);
        
        Path outputPath = Paths.get(slicerPath.toString(), "testTempDir" + File.separator + "slice.log");
        List<String> out = Files.readAllLines(outputPath);
        System.out.println(out);
        
        assertEquals(Arrays.asList(
        "Main:13",
        "Main:9"), 
        out);
    }


    @Test
    void issue5() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue5");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test-issue5-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "TreeAdd", "-l 2");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 107;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList(
            "TreeAdd:68",
            "TreeNode:50",
            "TreeNode:102",
            "TreeAdd:67",
            "TreeNode:60",
            "TreeNode:101",
            "TreeNode:104",
            "TreeAdd:36",
            "TreeNode:103",
            "TreeNode:106",
            "TreeNode:105",
            "TreeAdd:70",
            "TreeAdd:62",
            "TreeNode:49",
            "TreeAdd:40",
            "TreeAdd:72",
            "TreeNode:59",
            "TreeAdd:53",
            "TreeAdd:33",
            "TreeAdd:65"));

        assertEquals(expected, sliceLines);
    }
    
    @Test
    void directStatementDependency() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue1");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test1-issue-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Main", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 15;
        Map<StatementInstance, String> slideDeps = TestUtils.sliceAndGetDirectDepdendeincesMap(slicer, dcfg, tracePositionToSliceFrom);
        
        Map<StatementInstance, String> expected = new HashMap<>();
        expected.put(dcfg.mapNoUnits(15), "start");
        expected.put(dcfg.mapNoUnits(14), "data, varaible:stack5, source:15");
        expected.put(dcfg.mapNoUnits(13), "data, varaible:stack4, source:15");
        expected.put(dcfg.mapNoUnits(11), "data, varaible:stack3, source:14");

        assertEquals(expected, slideDeps);
    }


    @Test
    void testFieldsFramework() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-fields-framework");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test-fields-framework-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Main", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 21;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList(
            "Main:18",
            "Main:17",
            "Main:9",
            "Main:8",
            "Main:13",
            "Main:7",
            "Main:5"
            ));

        assertEquals(expected, sliceLines);
    }


    @Test
    void issue10() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue10");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test-issue10-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Issue", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 10;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList(
            "Issue:10",
            "Issue:9",
            "Issue:6"));

        assertEquals(expected, sliceLines);
    }
    

    @Test
    void issue11() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue11");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test-issue11-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Issue", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 30;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList(
            "Issue:10",
            "Issue:9",
            "Pair:49",
            "Pair:44",
            "Pair:43",
            "Pair:42",
            "PairSet:33",
            "PairSet:31",
            "PairSet:30",
            "PairSet:29",
            "PairSet:24",
            "PairSet:23",
            "PairSet:19",
            "PairSet:18"));

        assertEquals(expected, sliceLines);
    }


    @Test
    void sliceMe_test() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "SliceMe");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "SliceMe-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "SliceMe", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 5;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("SliceMe:9", "SliceMe:7", "SliceMe:4"));

        assertEquals(expected, sliceLines);
    }




    @Test
    void conditions() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-conditions");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test-conditions-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 83;
        List<String> sliceLines = new ArrayList<>(TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom)).stream()
                                                .sorted((a, b) -> a.compareTo(b))
                                                .collect(Collectors.toList());

        List<String> expected = Arrays.asList("Bench:29", "Bench:26", "Bench:25", "Bench:22", "Bench:21", "Bench:17", "Bench:16", "Bench:18", "Bench:15", "Bench:10", "Bench:7", "Bench:4").stream()
                                                .sorted((a, b) -> a.compareTo(b))
                                                .collect(Collectors.toList());

        assertEquals(expected, sliceLines);
    }
}
