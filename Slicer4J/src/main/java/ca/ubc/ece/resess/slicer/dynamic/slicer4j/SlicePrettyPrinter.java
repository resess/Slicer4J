package ca.ubc.ece.resess.slicer.dynamic.slicer4j;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;

import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AccessPath;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.attribute.ForNodeLink;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizException;
import guru.nidi.graphviz.engine.Rasterizer;
import guru.nidi.graphviz.model.MutableGraph;
import static guru.nidi.graphviz.model.Factory.*;
import soot.toolkits.scalar.Pair;

/**
 * Function-level slicing output support.
 *
 * <p>Adds structured, human-readable slice reports that group results
 * by method rather than individual source lines. Used in conjunction
 * with the {@code --function-slice} CLI option.
 */
public class SlicePrettyPrinter {

    private SlicePrettyPrinter() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Prints the slice results grouped by method signature.
     *
     * <p>Function-level slicing contribution: produces a method-grained view
     * of the backward slice, where each method lists the source lines that
     * belong to it. Output is written to {@code slice-function.log}.
     *
     * @param outDir output directory for the report file
     * @param slice  the completed dynamic slice
     */
    public static void printFunctionSlice(String outDir, DynamicSlice slice) {
        Map<String, Set<String>> methodToLines = new LinkedHashMap<>();
        Map<String, String> methodToFile = new LinkedHashMap<>();

        for (Pair<Pair<StatementInstance, AccessPath>, Pair<StatementInstance, AccessPath>> entry : slice) {
            StatementInstance si = entry.getO1().getO1();
            if (si == null) {
                continue;
            }
            String sig = si.getMethod().getSignature();
            methodToLines.computeIfAbsent(sig, k -> new LinkedHashSet<>())
                    .add(si.getJavaSourceFile() + ":" + si.getJavaSourceLineNo());
            methodToFile.putIfAbsent(sig, si.getJavaSourceFile());
        }

        List<String> output = new ArrayList<>();
        output.add("Function-level slice (" + methodToLines.size() + " methods)");
        output.add(repeat("=", 72));

        int idx = 1;
        for (Map.Entry<String, Set<String>> e : methodToLines.entrySet()) {
            output.add(String.format("[%d] %s", idx++, e.getKey()));
            output.add("    Source: " + methodToFile.getOrDefault(e.getKey(), "UNKNOWN"));
            output.add("    Lines: " + e.getValue().size());
            e.getValue().stream()
                    .sorted()
                    .forEach(l -> output.add("      " + l));
            output.add("");
        }

        String fileName = outDir + File.separator + "slice-function.log";
        try {
            FileUtils.writeLines(new File(fileName), output);
        } catch (IOException ex) {
            System.err.println("Error writing function slice: " + ex.getMessage());
        }
    }

    /**
     * Generates a function-level DOT graph of the slice as a PDF.
     *
     * <p>Function-level slicing contribution: groups slice nodes by their
     * containing method, producing a method-grained dependency graph where
     * each node is a method and edges represent cross-method data or control
     * dependencies. Output is written to {@code slice-graph-function.pdf}.
     *
     * @param outDir output directory for the graph file
     * @param slice  the completed dynamic slice
     */
    public static void printFunctionDotGraph(String outDir, DynamicSlice slice) {
        MutableGraph g = mutGraph("Function-Level Slice").setDirected(true);

        // Collect unique method signatures and their display names
        Map<String, String> methodLabels = new LinkedHashMap<>();
        for (Pair<Pair<StatementInstance, AccessPath>, Pair<StatementInstance, AccessPath>> entry : slice) {
            StatementInstance dest = entry.getO1().getO1();
            StatementInstance src = entry.getO2().getO1();
            if (dest != null) {
                methodLabels.putIfAbsent(dest.getMethod().getSignature(),
                        shortenSignature(dest.getMethod().getSignature()));
            }
            if (src != null) {
                methodLabels.putIfAbsent(src.getMethod().getSignature(),
                        shortenSignature(src.getMethod().getSignature()));
            }
        }

        // Build method-level edges, deduplicating (data overrides control)
        Map<String, String> methodEdges = new LinkedHashMap<>();
        for (Pair<Pair<StatementInstance, AccessPath>, Pair<StatementInstance, AccessPath>> entry : slice) {
            StatementInstance dest = entry.getO1().getO1();
            StatementInstance src = entry.getO2().getO1();
            if (dest == null || src == null) {
                continue;
            }
            String dm = dest.getMethod().getSignature();
            String sm = src.getMethod().getSignature();
            if (dm.equals(sm)) {
                continue; // skip intra-method edges
            }
            String edgeType = slice.getEdges(dest.getLineNo(), src.getLineNo());
            String key = sm + "->" + dm;
            // Data edges take priority over control edges
            if (!methodEdges.containsKey(key) || "data".equals(edgeType)) {
                methodEdges.put(key, edgeType);
            }
        }

        // Create nodes
        Map<String, guru.nidi.graphviz.model.Node> nodes = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : methodLabels.entrySet()) {
            guru.nidi.graphviz.model.Node n = node(e.getValue());
            nodes.put(e.getKey(), n);
            g.add(n);
        }

        // Create edges
        for (Map.Entry<String, String> e : methodEdges.entrySet()) {
            String[] parts = e.getKey().split("->");
            String srcSig = parts[0];
            String destSig = parts[1];
            String edgeType = e.getValue();
            guru.nidi.graphviz.model.Node srcNode = nodes.get(srcSig);
            guru.nidi.graphviz.model.Node destNode = nodes.get(destSig);
            if (srcNode != null && destNode != null) {
                Style<ForNodeLink> edgeStyle = "control".equals(edgeType) ? Style.DASHED : Style.SOLID;
                g.add(srcNode.link(to(destNode).with(edgeStyle, Label.of(edgeType))));
            }
        }

        try {
            Graphviz.fromGraph(g).rasterize(Rasterizer.builtIn("pdf"))
                    .toFile(new File(outDir + File.separator + "slice-graph-function.pdf"));
        } catch (IOException | GraphvizException ex) {
            System.err.println("Error writing function dot graph: " + ex.getMessage());
        }
    }

    /**
     * Pretty-prints a human-readable slice report with method grouping,
     * source line listing, and inter-method dependency edges.
     *
     * <p>Function-level slicing contribution: produces a structured report
     * ({@code slice-pretty.log}) that helps developers understand which
     * methods participate in the slice and how they depend on each other.
     *
     * @param outDir       output directory for the report file
     * @param slice        the completed dynamic slice
     * @param slicePositions the trace positions used as slicing criterion
     * @param criterionStmts the StatementInstances used as slicing criterion
     */
    public static void printPrettySlice(String outDir, DynamicSlice slice,
            List<Integer> slicePositions, List<StatementInstance> criterionStmts) {

        // Group slice entries by destination method
        Map<String, List<StatementInstance>> methodGroups = new LinkedHashMap<>();
        Map<String, Set<String>> methodDeps = new LinkedHashMap<>();
        Map<String, String> methodToFile = new LinkedHashMap<>();
        Set<String> seenLines = new LinkedHashSet<>();

        for (Pair<Pair<StatementInstance, AccessPath>, Pair<StatementInstance, AccessPath>> entry : slice) {
            StatementInstance dest = entry.getO1().getO1();
            StatementInstance src = entry.getO2().getO1();
            if (dest == null || src == null) {
                continue;
            }

            String edgeType = slice.getEdges(dest.getLineNo(), src.getLineNo());
            String destMethod = dest.getMethod().getSignature();
            String srcMethod = src.getMethod().getSignature();

            methodGroups.computeIfAbsent(destMethod, k -> new ArrayList<>()).add(dest);
            methodToFile.putIfAbsent(destMethod, dest.getJavaSourceFile());

            if (!destMethod.equals(srcMethod)) {
                String depDesc = String.format("%s <-- %s (%s:%d)",
                        edgeType,
                        shortenSignature(srcMethod),
                        src.getJavaSourceFile(), src.getJavaSourceLineNo());
                methodDeps.computeIfAbsent(destMethod, k -> new LinkedHashSet<>()).add(depDesc);
            }
        }

        // Determine target method from criterion
        String targetMethod = "";
        if (criterionStmts != null && !criterionStmts.isEmpty() && criterionStmts.get(0) != null) {
            targetMethod = criterionStmts.get(0).getMethod().getSignature();
        }

        // Count unique source lines in the slice
        int totalLines = 0;
        for (List<StatementInstance> stmts : methodGroups.values()) {
            Set<String> unique = new LinkedHashSet<>();
            for (StatementInstance si : stmts) {
                unique.add(si.getJavaSourceFile() + ":" + si.getJavaSourceLineNo());
            }
            totalLines += unique.size();
        }

        // Build the report
        List<String> output = new ArrayList<>();
        output.add(repeat("=", 80));
        output.add("  SLICE REPORT");
        output.add("  Mode:           FUNCTION-LEVEL");
        output.add("  Target method:  " + targetMethod);
        output.add("  Criterion size: " + (criterionStmts != null ? criterionStmts.size() : 0) + " statements");
        output.add("  Slice size:     " + totalLines + " source lines across " + methodGroups.size() + " methods");
        output.add(repeat("=", 80));
        output.add("");

        // Per-method detail
        int idx = 1;
        for (Map.Entry<String, List<StatementInstance>> e : methodGroups.entrySet()) {
            String methodSig = e.getKey();
            output.add(repeat("-", 80));
            output.add(String.format("[%d] %s", idx++, methodSig));
            output.add("    Source: " + methodToFile.getOrDefault(methodSig, "UNKNOWN"));

            // Deduplicate and sort source lines
            Set<String> uniqueLines = new LinkedHashSet<>();
            for (StatementInstance si : e.getValue()) {
                uniqueLines.add(si.getJavaSourceFile() + ":" + si.getJavaSourceLineNo());
            }
            output.add("    Lines in slice (" + uniqueLines.size() + "):");
            uniqueLines.stream()
                    .sorted()
                    .forEach(l -> output.add("      " + l));

            // Dependencies on other methods
            Set<String> deps = methodDeps.get(methodSig);
            if (deps != null && !deps.isEmpty()) {
                output.add("    Inter-method dependencies:");
                deps.stream().sorted().forEach(d -> output.add("      " + d));
            }
            output.add("");
        }

        // Method-level dependency summary
        Map<String, Set<String>> methodGraph = new LinkedHashMap<>();
        for (Pair<Pair<StatementInstance, AccessPath>, Pair<StatementInstance, AccessPath>> entry : slice) {
            StatementInstance dest = entry.getO1().getO1();
            StatementInstance src = entry.getO2().getO1();
            if (dest == null || src == null) {
                continue;
            }
            String dm = dest.getMethod().getSignature();
            String sm = src.getMethod().getSignature();
            if (!dm.equals(sm)) {
                String edgeType = slice.getEdges(dest.getLineNo(), src.getLineNo());
                methodGraph.computeIfAbsent(dm, k -> new LinkedHashSet<>())
                        .add(shortenSignature(sm) + " (" + edgeType + ")");
            }
        }

        if (!methodGraph.isEmpty()) {
            output.add(repeat("=", 80));
            output.add("  METHOD-LEVEL DEPENDENCY GRAPH");
            output.add(repeat("=", 80));
            for (Map.Entry<String, Set<String>> e : methodGraph.entrySet()) {
                String caller = shortenSignature(e.getKey());
                for (String callee : e.getValue()) {
                    output.add("  " + caller + "  -->  " + callee);
                }
            }
            output.add("");
        }

        output.add(repeat("=", 80));
        output.add("  End of report");
        output.add(repeat("=", 80));

        String fileName = outDir + File.separator + "slice-pretty.log";
        try {
            FileUtils.writeLines(new File(fileName), output);
        } catch (IOException ex) {
            System.err.println("Error writing pretty slice: " + ex.getMessage());
        }
    }

    /**
     * Shortens a Soot method signature for display.
     * Converts {@code <com.example.Foo: void bar(int)>} to {@code Foo.bar()}.
     */
    private static String shortenSignature(String sig) {
        if (sig == null || sig.isEmpty()) {
            return sig;
        }
        String inner = sig.startsWith("<") && sig.endsWith(">") ? sig.substring(1, sig.length() - 1) : sig;
        int colon = inner.lastIndexOf(':');
        if (colon < 0) {
            return inner;
        }
        String className = inner.substring(0, colon);
        String methodPart = inner.substring(colon + 1).trim();

        // Extract short class name
        String shortClass = className;
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0) {
            shortClass = className.substring(lastDot + 1);
        }

        // Extract method name (before the first space, which separates return type)
        String methodName = methodPart;
        int firstSpace = methodPart.indexOf(' ');
        if (firstSpace >= 0) {
            methodName = methodPart.substring(firstSpace + 1);
        }

        // Remove parameter types for brevity
        int parenOpen = methodName.indexOf('(');
        if (parenOpen >= 0) {
            methodName = methodName.substring(0, parenOpen) + "()";
        }

        return shortClass + "." + methodName;
    }

    private static String repeat(String s, int count) {
        StringBuilder sb = new StringBuilder(s.length() * count);
        for (int i = 0; i < count; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
