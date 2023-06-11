package ca.ubc.ece.resess.slicer.dynamic.slicer4j;


import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;


public class Defects4JTest {

    static Path root = Paths.get(".").normalize().toAbsolutePath();
    static Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    static Path outDir = Paths.get(slicerPath.toString(), "defects4JTestDir");
    static Path sliceLogger = Paths.get(root.getParent().getParent().toString(), "DynamicSlicingCore" + File.separator + "DynamicSlicingLoggingClasses" + File.separator + "DynamicSlicingLogger.jar");
    static Slicer slicer = null;
    static DynamicControlFlowGraph defectsDcfg = null;

    @BeforeAll 
    static void preCleanUp() throws IOException, InterruptedException {
        TestUtils.cleanWorkingDirectory();
        Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "defects37");
        String jarPath = Paths.get(testPath.toString(), "program.jar").toString();

        slicer = TestUtils.setupSlicing(root, jarPath, outDir, sliceLogger);
        if( !Files.exists( outDir ) ) {
            String instrumentedJar = slicer.instrument();
            TestUtils.runInstrumentedJarFromTest(instrumentedJar, instrumentedJar, "org.apache.commons.math.complex.ComplexTest", "testTanhInf", String.valueOf(outDir));
        }

        defectsDcfg = slicer.prepareGraph();
    }
    
    @AfterEach 
    void postCleanUp() throws IOException {
        TestUtils.cleanWorkingDirectory();
    }

    @Test
    void dcfgMappingBug1() throws IOException, InterruptedException {
        // goto statement as well
        List<Integer> traceLines = TestUtils.getTracePositionFromSourceLine( 866, "org.apache.commons.math.util.FastMath", defectsDcfg );
        slicer.printGraph( defectsDcfg );
        assert(traceLines.size() == 1);
    }

    @Test
    void dcfgMappingBug2() throws IOException, InterruptedException {
        // need to deal with goto statements that use "tmpString = x" ?
        List<Integer> traceLines = TestUtils.getTracePositionFromSourceLine( 2196, "org.apache.commons.math.util.FastMath", defectsDcfg );
        slicer.printGraph( defectsDcfg );
        assert(traceLines.size() == 1);
    }

    @Test
    void controlFlowBug1() throws IOException, InterruptedException {
        List<Integer> traceLines = TestUtils.getTracePositionFromSourceLine( 1054, "org.apache.commons.math.complex.ComplexTest", defectsDcfg );
        assert(traceLines.size() != 0);
        slicer.printGraph( defectsDcfg );

        List<String> sliceLines = new ArrayList<>(TestUtils.sliceAndGetSourceLines(slicer, defectsDcfg, traceLines)).stream()
                .sorted((a, b) -> a.compareTo(b))
                .collect(Collectors.toList());

        Assertions.assertTrue(sliceLines.contains("org.apache.commons.math.util.FastMath:2195"));
    }

    @Test
    void controlFlowBug2() throws IOException, InterruptedException {
        List<Integer> traceLines = TestUtils.getTracePositionFromSourceLine( 1054, "org.apache.commons.math.complex.ComplexTest", defectsDcfg );
        assert(traceLines.size() != 0);
        slicer.printGraph( defectsDcfg );

        List<String> sliceLines = new ArrayList<>(TestUtils.sliceAndGetSourceLines(slicer, defectsDcfg, traceLines)).stream()
                .sorted((a, b) -> a.compareTo(b))
                .collect(Collectors.toList());

        Assertions.assertTrue(sliceLines.contains("org.apache.commons.math.util.FastMath:2281"));
    }

    @Test
    void controlFlowBug3() throws IOException, InterruptedException {
        List<Integer> traceLines = TestUtils.getTracePositionFromSourceLine( 1054, "org.apache.commons.math.complex.ComplexTest", defectsDcfg );
        assert(traceLines.size() != 0);
        slicer.printGraph( defectsDcfg );

        List<String> sliceLines = new ArrayList<>(TestUtils.sliceAndGetSourceLines(slicer, defectsDcfg, traceLines)).stream()
                .sorted((a, b) -> a.compareTo(b))
                .collect(Collectors.toList());

        Assertions.assertTrue(sliceLines.contains("org.apache.commons.math.util.FastMath:826"));
    }

    @Test
    void controlFlowBug4() throws IOException, InterruptedException {
        List<Integer> traceLines = TestUtils.getTracePositionFromSourceLine( 1054, "org.apache.commons.math.complex.ComplexTest", defectsDcfg );
        assert(traceLines.size() != 0);
        slicer.printGraph( defectsDcfg );

        List<String> sliceLines = new ArrayList<>(TestUtils.sliceAndGetSourceLines(slicer, defectsDcfg, traceLines)).stream()
                .sorted((a, b) -> a.compareTo(b))
                .collect(Collectors.toList());

        Assertions.assertTrue(sliceLines.contains("org.apache.commons.math.util.FastMath:864"));
    }
}
