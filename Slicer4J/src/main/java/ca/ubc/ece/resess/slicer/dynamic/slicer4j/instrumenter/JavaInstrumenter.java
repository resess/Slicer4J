package ca.ubc.ece.resess.slicer.dynamic.slicer4j.instrumenter;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import soot.Body;
import soot.BodyTransformer;
import soot.PackManager;
import soot.PatchingChain;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Transform;
import soot.Trap;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.InvokeStmt;
import soot.jimple.ReturnStmt;
import soot.jimple.ReturnVoidStmt;
import soot.jimple.SpecialInvokeExpr;
import soot.options.Options;
import soot.util.Chain;
import soot.jimple.GotoStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.ThrowStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import ca.ubc.ece.resess.slicer.dynamic.core.instrumenter.InstrumenterUtils;
import ca.ubc.ece.resess.slicer.dynamic.core.instrumenter.Flags;
import ca.ubc.ece.resess.slicer.dynamic.core.instrumenter.AddedLocals;
import ca.ubc.ece.resess.slicer.dynamic.core.instrumenter.StmtSwitch;
import ca.ubc.ece.resess.slicer.dynamic.core.instrumenter.Instrumenter;
import ca.ubc.ece.resess.slicer.dynamic.core.instrumenter.InstrumentationCounter;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisLogger;
import ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer;

public class JavaInstrumenter extends Instrumenter {
    Set<String> threadMethods = new HashSet<>();
    private boolean fieldTracking = false;
    private boolean threadTracking = false;
    private boolean timeTracking = false;
    private boolean isOriginal = false;
    private Long jarSize = 0L;
    JSONObject staticLog = new JSONObject();
    InstrumentationCounter globalLineCounter = new InstrumentationCounter();
    Chain<SootClass> libClasses = null;
    String jarName;

    public JavaInstrumenter(String jarName) {
        // this.threadMethods.addAll(tc.values());
        this.jarName = new File(jarName).getAbsolutePath();
    }


    void initialize(String jarPath, String loggerJar) {
        AnalysisLogger.log(true, "Initializing Instrumenter");
        createInstrumentationPackagesList();
        Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
        Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
        Scene.v().addBasicClass("DynamicSlicingLogger", SootClass.BODIES);
        Scene.v().addBasicClass("DynamicSlicingLoggerWriter", SootClass.BODIES);
        Scene.v().addBasicClass("DynamicSlicingLoggerShutdown", SootClass.BODIES);
        Options.v().set_prepend_classpath(true);
        // Options.v().set_soot_classpath("VIRTUAL_FS_FOR_JDK");
        String [] excList = {"org.slf4j.impl.*", "org.mozilla.*"};
        List<String> excludePackagesList = Arrays.asList(excList);
        Options.v().set_exclude(excludePackagesList);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_process_dir(Arrays.asList(jarPath, loggerJar));
        Options.v().set_output_format(Options.output_format_class);
        Options.v().set_output_dir(Slicer.SOOT_OUTPUT_STRING);
        Options.v().setPhaseOption("jb", "use-original-names:true");
        libClasses = Scene.v().getLibraryClasses();
        AnalysisLogger.log(true, "Initialization done");
    }

    void runMethodTransformationPack() {
        PackManager.v().getPack("wjpp").add(new Transform("wjpp.classadder", new SceneTransformer(){
            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
                Scene.v().getSootClass("DynamicSlicingLogger").setApplicationClass();
                Scene.v().getSootClass("DynamicSlicingLoggerWriter").setApplicationClass();
                Scene.v().getSootClass("DynamicSlicingLoggerShutdown").setApplicationClass();
            }
        }));


        PackManager.v().getPack("jtp").add(new Transform("jtp.myInstrumenter", new BodyTransformer() {
            @Override
            protected void internalTransform(final Body b, String phaseName, @SuppressWarnings("rawtypes") Map options) {
                SootClass cls = b.getMethod().getDeclaringClass();
                if (cls.getName().contains("DynamicSlicingLogger")) {
                    return;
                }
                if (cls.getName().contains("DynamicSlicingLoggerWriter")) {
                    return;
                }
                if (cls.getName().contains("DynamicSlicingLoggerShutdown")) {
                    return;
                }

                boolean skip = !JavaInstrumenter.this.instrumentationPackagesList.isEmpty();
                for (String includedPkg: JavaInstrumenter.this.instrumentationPackagesList) {
                    if (cls.getPackageName().startsWith(includedPkg) ) {
                        skip = false;
                    }
                }
                if (skip) {
                    return;
                }

                
                Long methodSize = 0L;
                SootMethod mtd = b.getMethod();

                if (cls.getName().contains("OutputStream") && mtd.getName().contains("write")) {
                    return;
                }


                boolean isOnDestroy = false;
                if (mtd.getName().equals("onDestroy")) {
                    isOnDestroy = true;
                }
                StmtSwitch stmtSwitch = new StmtSwitch();
                
                stmtSwitch.setOriginal(isOriginal);
                AddedLocals addedLocals = new AddedLocals();
                Flags flags = new Flags(timeTracking, threadTracking, fieldTracking, false, false, isOriginal);
                if (mtd.getName().contains("<init>")) {
                    flags.superCalled = false;
                } else {
                    flags.superCalled = true;
                }
                if (b.getTraps().isEmpty()) {
                    stmtSwitch.setTimeTracking(timeTracking);
                } else {
                    flags.timeTracking = false;
                }


                
                List<String> traps = new ArrayList<>();
                for (Trap trap: mtd.getActiveBody().getTraps()) {
                    traps.add(trap.getBeginUnit().toString());
                }
                // AnalysisLogger.log(true, "Traps of {} are {}", mtd, traps); 

                final PatchingChain<Unit> units = b.getUnits();
                Set<Unit> instrumentedUnits = new HashSet<>();
                boolean instrumentedFirst = false;
                LinkedHashMap<Unit, Long> unitNumMap = new LinkedHashMap<>();
                Map<Unit, Long> taggedUnits = new HashMap<>();
                // AnalysisLogger.log(true, "In method: {}", mtd);
                for(Iterator<Unit> iter = units.snapshotIterator(); iter.hasNext();) {
                    final Unit u = (Unit) iter.next();
                    if (!(u instanceof IdentityStmt)) {
                        instrumentedFirst = InstrumenterUtils.basicBlockInstrument(b, cls, mtd, isOnDestroy, addedLocals, flags, units,
                                                                instrumentedUnits, instrumentedFirst, unitNumMap, taggedUnits, u, traps,
                                                                globalLineCounter, threadMethods, libClasses);
                        methodSize += 1;
                    }
                    
                }
                synchronized (jarSize) {
                    jarSize += methodSize;
                }
                JSONObject job = new JSONObject();
                String key = "";
                JSONArray jArray = new JSONArray();
                Unit prevU = null;
                for (Unit u : unitNumMap.keySet()) {
                    if (u instanceof IdentityStmt) {
                        continue;
                    }
                    if (taggedUnits.containsKey(u)) {
                        if (!key.equals("")){
                            job.put(key, jArray);
                            jArray = new JSONArray();
                        }
                        key = taggedUnits.get(u).toString();
                    } else if (prevU != null && (prevU instanceof ReturnStmt || prevU instanceof ReturnVoidStmt || prevU instanceof ThrowStmt)) {
                        if (!key.equals("")){
                            job.put(key, jArray);
                            jArray = new JSONArray();
                        }
                        if (taggedUnits.get(u) == null) {
                            key = "";
                        } else {
                            key = taggedUnits.get(u).toString();
                        }
                    }
                    jArray.add(u.toString());
                    prevU = u;
                }
                if (!key.equals("")) {
                    job.put(key, jArray);
                }
                
                synchronized(staticLog){
                    staticLog.put(b.getMethod().getSignature(), job);
                }
            }
        }));
        
    }

    @Override
    public void start (String options, String staticLogFile, String jarPath, String loggerJar) {
        soot.G.reset();
        if (options.contains("field")) {
            this.fieldTracking = true;
        }
        if (options.contains("thread")) {
            this.threadTracking = true;
        }
        if (options.contains("time")) {
            this.timeTracking = true;
        }
        if (options.contains("original")) {
            this.isOriginal = true;
        }

        initialize(jarPath, loggerJar);
        runMethodTransformationPack();
        Scene.v().loadNecessaryClasses();
        AnalysisLogger.log(true, "Running packs ... ");
        PackManager.v().runPacks();
        AnalysisLogger.log(true, "Writing output ... ");
        PackManager.v().writeOutput();
        AnalysisLogger.log(true, "Output written ... ");

        AnalysisLogger.log(true, "Writing log file... ");
        File logFile = new File(staticLogFile);
        try {
            logFile.delete();
            FileUtils.writeStringToFile(logFile, staticLog.toString(), "UTF-8", true);
            
        } catch (IOException e) {
            throw new Error("Failed to write static log file");
        }



        AnalysisLogger.log(true, "Number of Jimple statements (jarSize): {}", jarSize.toString());
        AnalysisLogger.log(true, "Writing JAR");
        try {
            AnalysisLogger.log(true, "Soot file: {}", new File(Slicer.SOOT_OUTPUT_STRING));
            AnalysisLogger.log(true, "Soot file is directory?: {}", new File(Slicer.SOOT_OUTPUT_STRING).isDirectory());
            if (new File(Slicer.SOOT_OUTPUT_STRING).isDirectory()) {
                // File unzipDir = new File("unzip_dir");
                // unzipDir.mkdir();
                unzipTo(new File(jarPath), new File(Slicer.SOOT_OUTPUT_STRING));

                File dir = new File(jarName).getParentFile();
                if (!dir.exists()) {
                    dir.mkdirs();
                }


                AnalysisLogger.log(true, "Writing size file ... ");
                File sizeFile = new File(dir.getAbsolutePath() + "/jar-size.txt");
                try {
                    sizeFile.delete();
                    FileUtils.writeStringToFile(sizeFile, "Number of Jimple statements (jarSize): " + jarSize.toString(), "UTF-8", true);
                } catch (IOException e) {
                    throw new Error("Failed to write jar-size file");
                }

                List<String> instrumentedClasses = new ArrayList<>();
                listDirectory(new File(Slicer.SOOT_OUTPUT_STRING).getAbsolutePath()+1, Slicer.SOOT_OUTPUT_STRING, 0, instrumentedClasses);

                AnalysisLogger.log(true, "Number of classes: {}", instrumentedClasses.size());
                int numFiles = 100;
                for (int i = 0; i < instrumentedClasses.size(); i+=numFiles){
                    // String clazzFile = instrumentedClasses.get(i);
                    
                    int minIndex = Math.min(i+numFiles, instrumentedClasses.size());
                    String clazzFile = String.join(" ", instrumentedClasses.subList(i, minIndex));
                    String jarOptions;
                    if (i == 0) {
                        jarOptions = "cvf";
                    } else {
                        jarOptions = "uf";
                    }

                    Process p = Runtime.getRuntime().exec("jar " + jarOptions + " " + jarName + " " + clazzFile, null, new File(Slicer.SOOT_OUTPUT_STRING));
                    p.waitFor();
                    // String output = IOUtils.toString(p.getInputStream());
                    // String errorOutput = IOUtils.toString(p.getErrorStream());
                    // AnalysisLogger.log(true, "Process result {}", output + " " + errorOutput);
                }
                // Process p = Runtime.getRuntime().exec("jar cvf " + jarName + " " + String.join(" ", instrumentedClasses), null, new File(Slicer.SOOT_OUTPUT_STRING));
                // p.waitFor();
            }

        } catch (IOException | InterruptedException e) {
            AnalysisLogger.warn(true, "Couldn't create jar");
        }
        AnalysisLogger.log(true, "Instrumentation done: file wrote {}", jarName);
    }

    private void unzipTo(File fileZip, File unzipDir) throws IOException {
        byte[] buffer = new byte[1024];
        ZipInputStream zis = new ZipInputStream(new FileInputStream(fileZip));
        ZipEntry zipEntry = zis.getNextEntry();
        while (zipEntry != null) {
            // AnalysisLogger.log(true, "Zip entry: {}", zipEntry);
            if (!zipEntry.getName().endsWith("/") && !zipEntry.getName().endsWith(".class") && !zipEntry.getName().contains("META-INF")) {
                // AnalysisLogger.log(true, "Copying: {}", zipEntry);
                File newFile = new File(unzipDir + File.separator + zipEntry);
                File parent = newFile.getParentFile();
                parent.mkdirs();
                // write file content
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
            }
            zipEntry = zis.getNextEntry();
        }
        
        zis.closeEntry();
        zis.close();
	}


	public void listDirectory(String base, String dirPath, int level, List<String> files) {
        File dir = new File(dirPath);
        File[] firstLevelFiles = dir.listFiles();
        if (firstLevelFiles != null && firstLevelFiles.length > 0) {
            for (File aFile : firstLevelFiles) {
                if (aFile.isDirectory()) {
                    listDirectory(base, aFile.getAbsolutePath(), level + 1, files);
                } else {
                    files.add(aFile.getAbsolutePath().substring(base.length())); // .replace("$", "\\$"));
                }
            }
        }
    }
}
