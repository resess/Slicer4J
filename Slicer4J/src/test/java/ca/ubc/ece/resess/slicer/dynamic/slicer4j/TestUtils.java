package ca.ubc.ece.resess.slicer.dynamic.slicer4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.HashSet;

import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;



public class TestUtils {
    protected static Slicer setupSlicing(Path root, String jarPath, Path outDir, Path sliceLogger) {
        Slicer slicer = new Slicer();
        slicer.setPathJar(jarPath);
        slicer.setOutDir(outDir.toString());
        slicer.setLoggerJar(sliceLogger.toString());
        
        slicer.setFileToParse(outDir + File.separator + "trace.log");
        slicer.setStubDroidPath(root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual");
        slicer.setTaintWrapperPath(root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt");
        return slicer;
    }
    
    protected static Map<StatementInstance, String> sliceAndGetDirectDepdendeincesMap(Slicer slicer, DynamicControlFlowGraph dcfg, Integer tracePositionToSliceFrom) {
        StatementInstance stmt = dcfg.mapNoUnits(tracePositionToSliceFrom);
        DynamicSlice dynamicSlice = slicer.directStatementDependency(stmt, true, false);
        Map<StatementInstance, String> sliceDeps = dynamicSlice.getSliceDependenciesAsMap();
        System.out.println(sliceDeps);
        return sliceDeps;
    }

    protected static Set<String> sliceAndGetSourceLines(Slicer slicer, DynamicControlFlowGraph dcfg, Integer tracePositionToSliceFrom) {
        StatementInstance stmt = dcfg.mapNoUnits(tracePositionToSliceFrom);
        DynamicSlice dynamicSlice = slicer.slice(dcfg, true, false, false, false, stmt, new HashSet<>(), slicer.getWorkingSet());
        Set<String> sliceLines = dynamicSlice.getSliceAsSourceLineNumbers();
        System.out.println(sliceLines);
        return sliceLines;
    }

    protected static void buildJar(Path testPath) throws IOException, InterruptedException {
        Process p = null;
        ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
        pb.directory(testPath.toFile());
        p = pb.start();
        p.waitFor();
        System.out.println("Out stream: ");
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String readline;
        while ((readline = reader.readLine()) != null) {
            System.out.println(readline);
        }
        reader.close();
        System.out.println("Error stream: ");
        reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
        while ((readline = reader.readLine()) != null) {
            System.out.println(readline);
        }
        reader.close();
    }
    
    protected static void cleanWorkingDirectory() throws IOException {
        Path root = Paths.get(".").normalize().toAbsolutePath();
        Path workingDirPath = Paths.get(root.getParent().toString(), "scripts" + File.separator + "testTempDir");
        if (workingDirPath.toFile().exists()) {
            Files.walk(workingDirPath)
            .sorted(Comparator.reverseOrder())
            .map(Path::toFile)
            .forEach(File::delete);
        }
    }
    
}
