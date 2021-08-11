import os
from argparse import ArgumentParser

script_dir = os.path.dirname(os.path.realpath(__file__))
slicer4j_dir = os.path.dirname(script_dir)
logger_jar = os.path.dirname(slicer4j_dir) + "/DynamicSlicingCore/DynamicSlicingLoggingClasses/DynamicSlicingLogger.jar"


def is_int(val) -> bool:
    try:
        int(val)
        return True
    except ValueError:
        return False


def main():
    options = parse()

    backward_criterion = options["backward_criterion"]

    jar_file = options["jar_file"]
    if not os.path.isfile(jar_file):
        print("Jar file does not exist!")
        return
    jar_file = os.path.abspath(jar_file)

    out_dir = options["out_dir"]
    if not os.path.isdir(out_dir):
        print(f"{out_dir} does not exist, creating it")
        os.makedirs(out_dir)
    out_dir = os.path.abspath(out_dir)

    dependencies = options["dependencies"]
    if dependencies and not os.path.isdir(dependencies):
        print(f"Dependencies directory doesn't exist")
        return
    if dependencies:
        dependencies = os.path.abspath(options["dependencies"])

    test_class = options["test_class"]
    test_method = options["test_method"]
    main_class_args = options["main_class_args"]
    framework_models = options["framework_models"]

    extra_options = ""
    if options["debug"]:
        extra_options += "-d "
    if options["data_only"]:
        extra_options += "-data "
    if options["ctrl_only"]:
        extra_options += "-ctrl "
    if options["once"]:
        extra_options += "-once "
    if options["data_only"] and options["ctrl_only"]:
        print("Conflicting arguments: data-only and control-only!")
        return
    if (not main_class_args) and (not test_class or not test_method):
        print("Must provide either main class name and arguments or test class and test method")
        return
    if framework_models:
        extra_options += "-f " + framework_models

    instrumented_jar = instrument(jar_file=jar_file, out_dir=out_dir)
    print(f"Instrumented jar is at: {instrumented_jar}", flush=True)
    run(instrumented_jar, dependencies, out_dir, test_class, test_method, main_class_args)
    log_file, slice_graph = dynamic_slice(jar_file=jar_file, out_dir=out_dir, backward_criterion=backward_criterion,
                                          extra_options=extra_options)

    print(f"Slice source code lines: {out_dir}/slice.log")
    print(f"Raw slice: {out_dir}/raw-slice.log")
    print(f"Slice graph: {slice_graph}")
    print(f"Slice with dependencies: {out_dir}/slice-dependencies.log")


def instrument(jar_file: str, out_dir: str) -> str:
    instr_file = "instr-debug.log"
    print("Instrumenting the JAR", flush=True)
    instr_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/Slicer4J/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/Slicer4J/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m i -j {jar_file} -o {out_dir}/ -sl {out_dir}/static_log.log -lc {logger_jar} > {out_dir}/{instr_file} 2>&1"
    os.system(instr_cmd)
    instrumented_jar = os.path.basename(jar_file).replace(".jar", "_i.jar")
    return out_dir + os.sep + instrumented_jar


def run(instrumented_jar, dependencies, out_dir, test_class, test_method, main_class_args):
    print("Running the instrumented JAR", flush=True)
    if main_class_args is None:
        cmd = f"java -Xmx8g -cp \"{script_dir}/SingleJUnitTestRunner.jar:{script_dir}/junit-4.8.2.jar:{instrumented_jar}:{dependencies}/*\" SingleJUnitTestRunner {test_class}#{test_method} > {out_dir}/trace_full.log"
    else:
        if main_class_args.startswith("\"") and main_class_args.endswith("\""):
            main_class_args = main_class_args[1:-1]
        cmd = f"java -Xmx8g -cp \"{instrumented_jar}:{dependencies}/*\" {main_class_args} > {out_dir}/trace_full.log"
    print(f"Running instrumented JAR", flush=True)
    print(f"------------------------------------")
    os.system(cmd)
    print(f"------------------------------------")
    os.system(f"cat {out_dir}/trace_full.log | grep \"SLICING\" > {out_dir}/trace.log")
    trace = list()
    with open(f"{out_dir}/trace.log", 'r') as f:
        for line in f:
            if "FIELD" in line:
                del trace[-1]
            trace.append(line.rstrip())

    with open(f"{out_dir}/trace.log", 'w') as f:
        for t in trace:
            f.write(t + "\n")


def dynamic_slice(jar_file=None, out_dir=None, backward_criterion=None, variables=None, extra_options=""):
    slice_file = "slice-file.log"
    graph_file = "graph-debug.log"
    if variables:
        print(f"Slicing from line {backward_criterion} with variables {variables}", flush=True)
    else:
        print(f"Slicing from line {backward_criterion}", flush=True)
    graph_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/Slicer4J/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/Slicer4J/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m g -j {jar_file} -t {out_dir}/trace.log -o {out_dir}/ -sl {out_dir}/static_log.log -sd {slicer4j_dir}/models/summariesManual -tw {slicer4j_dir}/models/EasyTaintWrapperSource.txt > {out_dir}/{graph_file} 2>&1"
    os.system(graph_cmd)

    clazz, lineno = backward_criterion.split(":")
    # clazz = clazz.rsplit(".", 1)[0]
    check_str = f":LINENO:{lineno}:FILE:{clazz}"
    slice_line = ""
    with open(f"{out_dir}/trace.log_icdg.log", 'r') as f:
        for line in f:
            if check_str in line:
                if slice_line:
                    slice_line = slice_line + "-"
                slice_line = slice_line + line.rstrip().split(", ")[0]

    if variables:
        extra_options += "-sv " + str(variables)

    slice_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/Slicer4J/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/Slicer4J/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m s -j {jar_file} -t {out_dir}/trace.log -o {out_dir}/ -sl {out_dir}/static_log.log -sd {slicer4j_dir}/models/summariesManual -tw {slicer4j_dir}/models/EasyTaintWrapperSource.txt -sp {slice_line} {extra_options} > {out_dir}/{slice_file} 2>&1"
    os.system(slice_cmd)
    arr = [x for x in os.listdir(out_dir) if x.startswith("result_md")]
    for a in arr:
        os.system(f"rm {out_dir}/{a}")
    return f"{out_dir}/{slice_file}", f"{out_dir}/slice-graph.pdf"


def parse():
    parser = ArgumentParser()
    parser.add_argument("-j", "--jar_file", dest="jar_file",
                        help="JAR file", metavar="path/to/jar", required=True)
    parser.add_argument("-o", "--out_dir", dest="out_dir",
                        help="Output folder", metavar="path/to/out/folder", required=True)
    parser.add_argument("-b", "--backward-criterion", dest="backward_criterion",
                        help="Backward criterion (line number)", metavar="line to slice backward from", required=True)
    parser.add_argument("-v", "--variables", dest="variables",
                        help="Variables to slice from, list of - separated names", metavar="variables to slice from",
                        required=False)
    parser.add_argument("-mod", "--models", dest="framework_models",
                        help="Folder containing user-defined method models", metavar="user defined framework models",
                        required=False)
    parser.add_argument("-debug", "--debug", dest="debug",
                        help="Enable debug", action='store_true', required=False)
    parser.add_argument("-d", "--data", dest="data_only",
                        help="Slice with data-flow dependencies only", action='store_true', required=False)
    parser.add_argument("-c", "--control", dest="ctrl_only",
                        help="Slice with control dependencies only", action='store_true', required=False)
    parser.add_argument("-once", "--once", dest="once",
                        help="Slice for first statement only then stop", action='store_true', required=False)
    parser.add_argument("-tc", "--test-class", dest="test_class",
                        help="Test class to run", metavar="name", required=False)
    parser.add_argument("-tm", "--test-method", dest="test_method",
                        help="Test method to run", metavar="name", required=False)
    parser.add_argument("-m", "--main-class-args", dest="main_class_args",
                        help="Main class to run with arguments", metavar="name", required=False)
    parser.add_argument("-dep", "--dependencies", dest="dependencies",
                        help="JAR dependencies", metavar="path", required=False)
    args = parser.parse_args()
    return {
        "jar_file": args.jar_file, "out_dir": args.out_dir, "backward_criterion": args.backward_criterion,
        "variables": args.variables, "data_only": args.data_only, "ctrl_only": args.ctrl_only,
        "test_class": args.test_class, "test_method": args.test_method, "main_class_args": args.main_class_args,
        "dependencies": args.dependencies, "framework_models": args.framework_models, "debug": args.debug, "once": args.once
    }


if __name__ == "__main__":
    main()
