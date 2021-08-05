
import os
import time

file_path = os.path.dirname(os.path.realpath(__file__))
slicer4j_dir = file_path + os.path.sep + ".." + os.path.sep + "Slicer4J" + os.path.sep
dynamic_slicing_core = file_path + os.path.sep + ".." + os.path.sep + ".." + os.path.sep + "DynamicSlicingCore" + os.path.sep
javaslicer_dir = file_path + os.path.sep + ".." + os.path.sep + ".." + os.path.sep + "javaslicer" + os.path.sep

benchmarks_input = {
    "javaslicer-bench1-intra-procedural" : ("target/javaslicer-bench1-intra-procedural-1.0.0.jar", "Bench 2 ", "Bench", 8),
    "javaslicer-bench2-inter-procedural" : ("target/javaslicer-bench2-inter-procedural-1.0.0.jar", "Bench 2 4 ", "Bench", 8),
    "javaslicer-bench3-exceptions" : ("target/javaslicer-bench3-exceptions-1.0.0.jar", "Bench ", "Bench", 29),
    "slicer4j-bench1-multiple-threads": ("target/slicer4j-bench1-multiple-threads-1.0.0.jar", "Bench ", "Bench", 14), 
    "slicer4j-bench2-native-framework": ("target/slicer4j-bench2-native-framework-1.0.0.jar", "Bench 0 4", "Bench", 9),
    "slicer4j-bench3-java-9-constructs": ("target/slicer4j-bench3-java-9-constructs-1.0.0.jar", "Bench 3 4", "Bench", 9),
    "slicer4j-bench4-instrumentation-classes": ("target/slicer4j-bench4-instrumentation-classes-1.0.0.jar", "Bench 22 ", "Bench", 10),
    "slicer4j-bench5-static-constructor": ("target/slicer4j-bench5-static-constructor-1.0.0.jar", "Bench 22 ", "Bench", 8),
}

defects4j_benchmarks = {
    "JacksonDatabind_3b": ("target/jackson-databind-2.4.1-SNAPSHOT.jar", "org.junit.runner.JUnitCore com.fasterxml.jackson.databind.deser.TestArrayDeserialization", "com.fasterxml.jackson.databind.ObjectMapper", "3062", "_readMapAndClose", "JacksonDatabind_3b/target/test-classes/:JacksonDatabind_3b/target/dependency/*"),
    "Gson_4b": ("gson/target/gson-2.6-SNAPSHOT.jar", "junit.textui.TestRunner com.google.gson.stream.JsonReaderTest", "com.google.gson.stream.JsonReader", "1422", "checkLenient", "Gson_4b/gson/target/test-classes/:Gson_4b/gson/target/dependency/*"), 
    "JacksonCore_4b": ("target/jackson-core-2.5.0-SNAPSHOT.jar", "org.junit.runner.JUnitCore com.fasterxml.jackson.core.util.TestTextBuffer", "com.fasterxml.jackson.core.util.TextBuffer", "587", "expandCurrentSegment", "JacksonCore_4b/target/test-classes/:JacksonCore_4b/target/dependency/*"), 
}

def count_lines_slice_slicer4j(trace):
    trace_list = list()
    with open(trace + os.path.sep + "trace.log_icdg.log", 'r') as f:
        for line in f:
            trace_list.append(line.rstrip())
    print(f"Length of trace (Jimple LoC): {len(trace_list)}")

    all_locations = list()
    with open(trace + os.path.sep + "slice.log", 'r') as f:
        for line in f:
            all_locations.append(line.rstrip())

    return len(all_locations)


def count_lines_slice_javaslicer(trace):
    trace_list = list()
    start_recording = False
    with open(trace, 'r') as f:
        for line in f:
            if "The dynamic slice for criterion" in line:
                start_recording = True
            if "Slice consists" in line:
                start_recording = False
            if start_recording:
                trace_list.append(line.rstrip())
    print(f"Length of trace (bytecode LoC): {len(trace_list)}")
    all_locations = list()
    full_line = list()
    prev_src_location = None
    for trace_line in trace_list:
        if ":" in trace_line:
            src_location = trace_line.split(" ")[0]
            if any([x for x in ["The", "sun.", "org.junit", "junit.framework", "java.", "com.sun.", "junit."] if src_location.startswith(x)]):
                continue
            if src_location != prev_src_location:
                all_locations.append(src_location)
            full_line.append(trace_line+"\n")
        prev_src_location = src_location

    with open(f"{trace}.source", "w") as f:
        f.writelines(full_line)

    return len(all_locations)

def build_jar(project):
    cwd = os.getcwd()
    os.chdir(f"{project}")
    cmd = f"mvn clean package > /dev/null 2>&1"
    os.system(cmd)
    cmd = f"mvn -Dmaven.test.skip=true package > /dev/null 2>&1"
    os.system(cmd)
    cmd = "mvn dependency:copy-dependencies > /dev/null 2>&1"
    os.system(cmd)
    os.chdir(cwd)


def run_original(project, jar_name, project_arg, extra_libs):
    start = time.time()
    cwd = os.getcwd()
    os.chdir(f"{project}")
    cmd = f"java -cp \"{jar_name}:{extra_libs}\" {project_arg} > /dev/null 2>&1"
    # print(cmd)
    os.system(cmd)
    os.chdir(cwd)
    exec_time = time.time()
    print(f"Original exec time (s): {exec_time-start}")


def run_slicer4j(project, jar_name, project_arg, extra_libs, sc_file, slice_line):
    slice_file = "slice-result.log"
    cwd = os.getcwd()
    out_dir=f"results/{project}"
    start_instr = time.time()
    if os.path.isdir(out_dir):
        os.system(f"rm -r {out_dir}")
    os.mkdir(out_dir)
    instr_cmd = f"java -cp \"{slicer4j_dir}/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m i -j {jar_name} -o {out_dir}/ -sl {out_dir}/static-log.log -lc {dynamic_slicing_core}/DynamicSlicingLoggingClasses/DynamicSlicingLogger.jar > /dev/null 2>&1"
    os.system(instr_cmd)
    instr_time = time.time()
    print(f"Instrumentation time (s): {instr_time-start_instr}")
    instrumented_jar = os.path.basename(jar_name).replace(".jar", "_i.jar")
    cmd = f"java -cp \"{out_dir}/{instrumented_jar}:{extra_libs}\" {project_arg} | grep \"SLICING\" > {out_dir}/trace.log"
    # print(cmd)
    os.system(cmd)
    trace = list()
    with open(f"{out_dir}/trace.log", 'r') as f:
        for l in f:
            if "FIELD" in l:
                del trace[-1]
            trace.append(l.rstrip())
    with open(f"{out_dir}/trace.log", 'w') as f:
        for t in trace:
            f.write(t+"\n")
    run_time = time.time()
    print(f"Execution time (s): {run_time-instr_time}")
    graph_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m g -j {jar_name} -t {out_dir}/trace.log -o {out_dir}/ -sl {out_dir}/static-log.log -sd {slicer4j_dir}/../models/summariesManual -tw {slicer4j_dir}/../models/EasyTaintWrapperSource.txt > /dev/null 2>&1"
    # print(graph_cmd)
    os.system(graph_cmd)
    sc = None
    if not sc_file:
        with open(f"{out_dir}/trace.log_icdg.log", 'r') as f:
            for l in f:
                if f"println" in l:
                    sc = l.rstrip()
        line = sc.split(", ")[0]
    else:
        print(f"looking for LINENO:{slice_line}:FILE:{sc_file}")
        with open(f"{out_dir}/trace.log_icdg.log", 'r') as f:
            for l in f:
                if f"LINENO:{slice_line}:FILE:{sc_file}" in l:
                    sc = l.rstrip()
        line = sc.split(", ")[0]
    slice_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -j {jar_name} -m s -t {out_dir}/trace.log -o {out_dir}/ -sl {out_dir}/static-log.log -sd {slicer4j_dir}/../models/summariesManual -tw {slicer4j_dir}/../models/EasyTaintWrapperSource.txt -sp {line} -d > {out_dir}/{slice_file}_{line}.log 2>&1"
    os.system(slice_cmd)
    slice_time = time.time()
    print(f"Slice time (s): {slice_time-run_time}")
    num_lines = count_lines_slice_slicer4j(f"{out_dir}")
    print(f"Slice size (Java LoC): {num_lines}")

def run_javaslicer(project, jar_name, project_arg, extra_libs, sc_file, slice_line, slice_method):
    start = time.time()
    tracer_cmd = f"java -javaagent:{javaslicer_dir}/assembly/tracer.jar=tracefile:results/{project}/{project}.trace -cp \"{jar_name}:{extra_libs}\" {project_arg} > /dev/null 2>&1"
    # print(tracer_cmd)
    os.system(tracer_cmd)
    exec_time = time.time()
    print(f"Instr + exec time (s): {exec_time-start}")
    slicer_cmd = f"java -jar {javaslicer_dir}/assembly/slicer.jar -p results/{project}/{project}.trace {sc_file}.{slice_method}:{slice_line}:* > results/{project}/{project}.javaslicer 2>&1"
    os.system(slicer_cmd)
    slice_time = time.time()
    print(f"Slice time (s): {slice_time-exec_time}")

    num_lines = count_lines_slice_javaslicer(f"results/{project}/{project}.javaslicer")
    print(f"Slice size (Java LoC): {num_lines}")


if not os.path.isdir("results"):
    os.mkdir("results")

for idx, project in enumerate(benchmarks_input):
    jar_name = f"{project}/"+benchmarks_input[project][0]
    project_arg = benchmarks_input[project][1]
    sc_file = benchmarks_input[project][2]
    slice_line = benchmarks_input[project][3]
    print(f"====================")
    print(f"Benchmark: {project}")
    build_jar(project)
    print(f"********************")
    run_original(project, jar_name, project_arg, "")
    print(f"********************")
    print(f"Running Slicer4J")
    run_slicer4j(project, jar_name, project_arg, "", "", "")
    print(f"********************")
    print(f"Running JavaSlicer")
    run_javaslicer(project, jar_name, project_arg, "", sc_file, slice_line, "main")


for idx, project in enumerate(defects4j_benchmarks):
    jar_name = f"{project}/"+defects4j_benchmarks[project][0]
    project_arg = defects4j_benchmarks[project][1]
    sc_file = defects4j_benchmarks[project][2]
    slice_line = defects4j_benchmarks[project][3]
    slice_method = defects4j_benchmarks[project][4]
    extra_libs = defects4j_benchmarks[project][5]
    print(f"====================")
    print(f"Benchmark: {project}")
    build_jar(project)
    print(f"********************")
    run_original(project, jar_name, project_arg, extra_libs)
    print(f"********************")
    print(f"Running Slicer4J")
    run_slicer4j(project, jar_name, project_arg, extra_libs, sc_file, slice_line)
    print(f"********************")
    print(f"Running JavaSlicer")
    run_javaslicer(project, jar_name, project_arg, extra_libs, sc_file, slice_line, slice_method)