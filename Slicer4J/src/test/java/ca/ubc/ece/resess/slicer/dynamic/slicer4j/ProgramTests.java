package ca.ubc.ece.resess.slicer.dynamic.slicer4j;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


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
    System.out.println("Test path:" + testPath.toString());
    Process p = null;
    ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
    pb.directory(testPath.toFile());
    p = pb.start();
    p.waitFor();
    Files.walk(testPath)
      .sorted(Comparator.reverseOrder())
      .map(Path::toFile)
      .forEach(System.out::println);
    Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    System.out.println("Slicer path:" + slicerPath.toString());
    String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test1-issue-1.0.0.jar").toString();
    System.out.println("Jar Path:" + jarPath);
    pb = new ProcessBuilder("python3", "slicer4j.py", "-j", jarPath, "-debug", "-o", "testTempDir", "-b", "Main:9", "-m", "\"Main\"");
    pb.directory(slicerPath.toFile());
    p = pb.start();
    p.waitFor();
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String readline;
    int i = 0;
    while ((readline = reader.readLine()) != null) {
        System.out.println(++i + " " + readline);
    }
    reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    while ((readline = reader.readLine()) != null) {
        System.out.println(++i + " " + readline);
    }

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
    Path testPath = Paths.get(root.getParent().toString(), "benchmarks" + File.separator + "test-issue2");
    if (!testPath.toFile().isDirectory()) {
      testPath = Paths.get(root.getParent().toString(), ".." + File.separator + "benchmarks" + File.separator + "test-issue2");
    }
    Process p = null;
    ProcessBuilder pb = new ProcessBuilder("mvn", "clean", "package");
    pb.directory(testPath.toFile());
    p = pb.start();
    p.waitFor();
    Path slicerPath = Paths.get(root.getParent().toString(), "scripts");
    String jarPath = Paths.get(testPath.toString(), "target" + File.separator + "test2-issue-1.0.0.jar").toString();
    System.out.println("Jar Path:" + jarPath);
    pb = new ProcessBuilder("python3", "slicer4j.py", "-j", jarPath, "-debug", "-o", "testTempDir", "-b", "Main:11", "-m", "\"Main something\"");
    pb.directory(slicerPath.toFile());
    p = pb.start();
    p.waitFor();
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String readline;
    int i = 0;
    while ((readline = reader.readLine()) != null) {
        System.out.println(++i + " " + readline);
    }
    reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
    while ((readline = reader.readLine()) != null) {
        System.out.println(++i + " " + readline);
    }

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
