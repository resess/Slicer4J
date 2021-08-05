package ca.ubc.ece.resess.slicer.dynamic.slicer4j;

import static org.junit.jupiter.api.Assertions.assertEquals;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.HashSet;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;


public class BenchTests {

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
    void javaslicer_bench1() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "javaslicer-bench1-intra-procedural");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "javaslicer-bench1-intra-procedural-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "2");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 7;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench:5", "Bench:4", "Bench:7", "Bench:6", "Bench:8"));

        assertEquals(expected, sliceLines);
    }

    @Test
    void javaslicer_bench2() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "javaslicer-bench2-inter-procedural");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "javaslicer-bench2-inter-procedural-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "2");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 10;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench:12", "Bench:4", "Bench:7", "Bench:6", "Bench:8"));

        assertEquals(expected, sliceLines);
    }

    @Test
    void javaslicer_bench3() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "javaslicer-bench3-exceptions");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "javaslicer-bench3-exceptions-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 21;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench:33", "Bench:19", "Bench:29", "Bench:4", "Bench:17", "Bench:28"));

        assertEquals(expected, sliceLines);
    }

    @Test
    void slicer4j_bench1() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "slicer4j-bench1-multiple-threads");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "slicer4j-bench1-multiple-threads-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 38;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench:12", "Producer:28", "Bench:14", "Consumer:43", "Consumer:40"));

        assertEquals(expected, sliceLines);
    }

    @Test
    void slicer4j_bench2() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "slicer4j-bench2-native-framework");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "slicer4j-bench2-native-framework-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "0 4");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 21;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench:5", "Bench:4", "Bench:7", "Bench:6", "Bench:9", "Bench:8"));

        assertEquals(expected, sliceLines);
    }

    @Test
    void slicer4j_bench3() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "slicer4j-bench3-java-9-constructs");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "slicer4j-bench3-java-9-constructs-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "3 4");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 23;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench$lambda_main_0__1:-1", "Bench:13", "Bench:5", "Bench:7", "Bench:6", "Bench:9", "Bench:8"));

        assertEquals(expected, sliceLines);
    }

    @Test
    void slicer4j_bench4() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "slicer4j-bench4-instrumentation-classes");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "slicer4j-bench4-instrumentation-classes-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "22");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 11;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench:10", "Bench:7", "Bench:6", "Bench:9", "Bench:8"));

        assertEquals(expected, sliceLines);
    }

    @Test
    void slicer4j_bench5() throws IOException, InterruptedException {
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "slicer4j-bench5-static-constructor");
        String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "slicer4j-bench5-static-constructor-1.0.0.jar").toString();
        
        TestUtils.buildJar(testPath);
        
        Slicer slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        slicer.setDebug(true);
        String instrumentedJar = slicer.instrument();
        slicer.runInstrumentedJarFromMain(instrumentedJar, "Bench", "22");
        
        DynamicControlFlowGraph dcfg = slicer.prepareGraph();
        slicer.printGraph(dcfg);
        
        Integer tracePositionToSliceFrom = 12;
        Set<String> sliceLines = TestUtils.sliceAndGetSourceLines(slicer, dcfg, tracePositionToSliceFrom);

        Set<String> expected = new HashSet<>(Arrays.asList("Bench:12", "Bench:2", "Bench:4", "Bench:7", "Bench:6", "Bench:8"));

        assertEquals(expected, sliceLines);
    }
}
