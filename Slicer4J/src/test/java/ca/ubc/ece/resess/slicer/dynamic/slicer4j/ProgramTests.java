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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Comparator;
import java.util.HashMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;


public class ProgramTests {


  @BeforeAll 
  static void preCleanUp() throws IOException {
    cleanWorkingDirectory();
  }

  @AfterEach 
  void postCleanUp() throws IOException {
    cleanWorkingDirectory();
  }


  @Test
  void issue1() throws IOException, InterruptedException {
    Path root = Paths.get(".").normalize().toAbsolutePath();
    System.out.println("Root:" + root.toString());
    Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue1");
    String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test1-issue-1.0.0.jar").toString();
    System.out.println("Test path:" + testPath.toString());
    Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    Path outDir = Paths.get(slicerPath.toString(), "testTempDir");
    Path sliceLogger = Paths.get(root.getParent().getParent().toString(), "DynamicSlicingCore" + File.separator + "DynamicSlicingLoggingClasses" + File.separator + "DynamicSlicingLogger.jar");
    Process p = null;
    ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
    pb.directory(testPath.toFile());
    p = pb.start();
    p.waitFor();
    
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

    pb = new ProcessBuilder("/bin/sh", "-c", "java -Xmx8g -cp " + outDir.toString() + File.separator + "test1-issue-1.0.0_i.jar Main | grep \"SLICING\"");
    System.out.println(pb.command());
    p = pb.start();
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
    Path root = Paths.get(".").normalize().toAbsolutePath();
    System.out.println("Root:" + root.toString());
    Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue2");
    String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test2-issue-1.0.0.jar").toString();
    System.out.println("Test path:" + testPath.toString());
    Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    Path outDir = Paths.get(slicerPath.toString(), "testTempDir");
    Path sliceLogger = Paths.get(root.getParent().getParent().toString(), "DynamicSlicingCore" + File.separator + "DynamicSlicingLoggingClasses" + File.separator + "DynamicSlicingLogger.jar");
    Process p = null;
    ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
    pb.directory(testPath.toFile());
    p = pb.start();
    p.waitFor();
    
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

    pb = new ProcessBuilder("/bin/sh", "-c", "java -Xmx8g -cp " + outDir.toString() + File.separator + "test2-issue-1.0.0_i.jar Main something | grep \"SLICING\"");
    System.out.println(pb.command());
    p = pb.start();
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
    Path root = Paths.get(".").normalize().toAbsolutePath();
    System.out.println("Root:" + root.toString());
    Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue1");
    String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test1-issue-1.0.0.jar").toString();
    System.out.println("Test path:" + testPath.toString());
    Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    Path outDir = Paths.get(slicerPath.toString(), "testTempDir");
    Path sliceLogger = Paths.get(root.getParent().getParent().toString(), "DynamicSlicingCore" + File.separator + "DynamicSlicingLoggingClasses" + File.separator + "DynamicSlicingLogger.jar");
    Process p = null;
    ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
    pb.directory(testPath.toFile());
    p = pb.start();
    p.waitFor();
    
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

    pb = new ProcessBuilder("/bin/sh", "-c", "java -Xmx8g -cp " + outDir.toString() + File.separator + "test1-issue-1.0.0_i.jar Main | grep \"SLICING\"");
    System.out.println(pb.command());
    p = pb.start();
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
  void directStatementDependency() throws IOException, InterruptedException {
    Path root = Paths.get(".").normalize().toAbsolutePath();
    System.out.println("Root:" + root.toString());
    Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue1");
    String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test1-issue-1.0.0.jar").toString();
    System.out.println("Test path:" + testPath.toString());
    Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    Path outDir = Paths.get(slicerPath.toString(), "testTempDir");
    Path sliceLogger = Paths.get(root.getParent().getParent().toString(), "DynamicSlicingCore" + File.separator + "DynamicSlicingLoggingClasses" + File.separator + "DynamicSlicingLogger.jar");
    Process p = null;
    ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
    pb.directory(testPath.toFile());
    p = pb.start();
    p.waitFor();

    Slicer slicer = new Slicer();
    slicer.setPathJar(jarPath);
    slicer.setOutDir(outDir.toString());
    slicer.setLoggerJar(sliceLogger.toString());
    
    slicer.setFileToParse(outDir + File.separator + "trace.log");
    slicer.setStubDroidPath(root.getParent().toString() + File.separator + "models" + File.separator + "summariesManual");
    slicer.setTaintWrapperPath(root.getParent().toString() + File.separator + "models" + File.separator + "EasyTaintWrapperSource.txt");
    
    String instrumentedJar = slicer.instrument();
    slicer.runInstrumentedJarFromMain(instrumentedJar, "Main", "");
    DynamicControlFlowGraph dcfg = slicer.prepareGraph();
    slicer.printGraph(dcfg);

    Integer tracePositionToSliceFrom = 15;
    StatementInstance stmt = dcfg.mapNoUnits(tracePositionToSliceFrom);

    DynamicSlice dynamicSlice = slicer.directStatementDependency(stmt, true, false);
    Map<StatementInstance, String> slideDeps = dynamicSlice.getSliceDependenciesAsMap();
    System.out.println(slideDeps);

    Map<StatementInstance, String> expected = new HashMap<>();
    expected.put(dcfg.mapNoUnits(15), "data, start");
    expected.put(dcfg.mapNoUnits(14), "data, varaible:stack5, source:15");
    expected.put(dcfg.mapNoUnits(13), "data, varaible:stack4, source:15");
    expected.put(dcfg.mapNoUnits(11), "data, varaible:a, source:14");

    assertEquals(expected, slideDeps);
  }

  static void cleanWorkingDirectory() throws IOException {
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
