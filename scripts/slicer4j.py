import os, sys
from argparse import ArgumentParser

script_dir = os.path.dirname(os.path.realpath(__file__))
slicer4j_dir = os.path.abspath(script_dir + os.sep + ".." + os.sep)
logger_jar = os.path.abspath("/Users/khaledea/data/DynamicSlicingCore/DynamicSlicingLoggingClasses/DynamicSlicingLogger.jar")

def is_int(val) -> bool:
    try:
        int(val)
        return True
    except ValueError:
        return False


def main():
    options = parse()

    backward_criterion = options["backward_criterion"]

    jarfile = options["jarfile"]
    if not os.path.isfile(jarfile):
        print("Jar file does not exist!")
        return
    jarfile = os.path.abspath(jarfile)

    outdir = options["outdir"]
    if not os.path.isdir(outdir):
        print(f"{outdir} does not exist, creating it")
        os.makedirs(outdir)
    outdir = os.path.abspath(outdir)

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
    if options["data_only"]:
        extra_options += "-data "
    if options["ctrl_only"]:
        extra_options += "-ctrl "
    if options["data_only"] and options["ctrl_only"]:
        print("Conflicting arguments: data-only and control-only!")
        return
    if (not main_class_args) and (not test_class or not test_method):
        print("Must proide either main class name and arguments or test class and test method")
        return
    if framework_models:
        extra_options += "-f " + framework_models

    instrumented_jar = instrument(jarfile = jarfile, outdir = outdir)
    print(f"Instrumented jar is at: {instrumented_jar}", flush=True)
    run(instrumented_jar, dependencies, outdir, test_class, test_method, main_class_args)
    log_file, slice_graph = dynamic_slice(jarfile = jarfile, outdir = outdir, backward_criterion = backward_criterion, 
                                          extra_options = extra_options)
    
    print(f"Slice source code lines: {outdir}/slice.log")
    print(f"Raw slice: {outdir}/raw-slice.log")
    print(f"Slice graph: {slice_graph}")


def instrument(jarfile: str, outdir: str) -> str:
    print("Instrumenting the JAR", flush=True)
    instr_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/Slicer4J/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/Slicer4J/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m i -j {jarfile} -o {outdir}/ -sl {outdir}/static_log.log -lc {logger_jar} > /dev/null 2>&1"
    os.system(instr_cmd)
    instrumented_jar = os.path.basename(jarfile).replace(".jar", "_i.jar")
    return outdir + os.sep + instrumented_jar


def run(instrumented_jar, dependencies, outdir, test_class, test_method, main_class_args):
    print("Running the instrumented JAR", flush=True)
    if main_class_args == None:
        cmd = f"java -Xmx8g -cp \"{script_dir}/SingleJUnitTestRunner.jar:{script_dir}/junit-4.8.2.jar:{instrumented_jar}:{dependencies}/*\" SingleJUnitTestRunner {test_class}#{test_method} | grep \"SLICING\" > {outdir}/trace.log"
    else:
        cmd = f"java -Xmx8g -cp \"{instrumented_jar}:{dependencies}/*\" {main_class_args} | grep \"SLICING\" > {outdir}/trace.log"
    print(f"Running instrumented JAR", flush=True)
    print(f"------------------------------------")
    os.system(cmd)
    print(f"------------------------------------")
    trace = list()
    with open(f"{outdir}/trace.log", 'r') as f:
        for l in f:
            if "FIELD" in l:
                del trace[-1]
            trace.append(l.rstrip())

    with open(f"{outdir}/trace.log", 'w') as f:
        for t in trace:
            f.write(t+"\n")



def dynamic_slice(jarfile=None, outdir=None, backward_criterion=None, variables = None, extra_options = ""):
    slice_file = "slice-file.log"
    if variables:
        print(f"Slicing from line {backward_criterion} with variables {variables}", flush=True)
    else:
        print(f"Slicing from line {backward_criterion}", flush=True)
    graph_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/Slicer4J/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/Slicer4J/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m g -j {jarfile} -t {outdir}/trace.log -o {outdir}/ -sl {outdir}/static_log.log -sd {slicer4j_dir}/FlowDroid/soot-infoflow-summaries/summariesManual -tw {slicer4j_dir}/FlowDroid/soot-infoflow/EasyTaintWrapperSource.txt > /dev/null 2>&1"
    os.system(graph_cmd)

    clazz, lineno = backward_criterion.split(":")
    # clazz = clazz.rsplit(".", 1)[0]
    with open(f"{outdir}/trace.log_icdg.log", 'r') as f:
        for l in f:
            if f":LINENO:{lineno}:FILE:{clazz}" in l:
                sc = l.rstrip()

    line = sc.split(", ")[0]

    if variables:
        extra_options += "-sv " + str(variables)

    slice_cmd = f"java -Xmx8g -cp \"{slicer4j_dir}/Slicer4J/target/slicer4j-jar-with-dependencies.jar:{slicer4j_dir}/Slicer4J/target/lib/*\" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -m s -j {jarfile} -t {outdir}/trace.log -o {outdir}/ -sl {outdir}/static_log.log -sd {slicer4j_dir}/FlowDroid/soot-infoflow-summaries/summariesManual -tw {slicer4j_dir}/FlowDroid/soot-infoflow/EasyTaintWrapperSource.txt -sp {line} {extra_options} > {outdir}/{slice_file} 2>&1"
    os.system(slice_cmd)
    arr = [x for x in os.listdir(outdir) if x.startswith("result_md")]
    for a in arr:
        os.system(f"rm {outdir}/{a}")
    return f"{outdir}/{slice_file}", f"{outdir}/slice-graph.pdf"



def parse():

    parser = ArgumentParser()
    parser.add_argument("-j", "--jarfile", dest="jarfile", 
                        help="JAR file", metavar="path/to/jar", required=True)
    parser.add_argument("-o", "--outdir", dest="outdir", 
                        help="Output folder", metavar="path/to/out/folder", required=True)
    parser.add_argument("-b", "--backward-criterion", dest="backward_criterion", 
                        help="Backward criterion (line number)", metavar="line to slice backward from", required=True)
    parser.add_argument("-v", "--variables", dest="variables", 
                        help="Variables to slice from, list of - separated names", metavar="variables to slice from", required=False)
    parser.add_argument("-mod", "--models", dest="framework_models",
                        help="Folder containing user-defined method models", metavar="user defined framework models", required=False)
    parser.add_argument("-d", "--data", dest="data_only", 
                        help="Slice with data-flow dependencies only", action='store_true', required=False)
    parser.add_argument("-c", "--control", dest="ctrl_only", 
                        help="Slice with control dependencies only", action='store_true', required=False)
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
        "jarfile": args.jarfile, "outdir": args.outdir, "backward_criterion": args.backward_criterion, "variables": args.variables,
        "data_only": args.data_only, "ctrl_only": args.ctrl_only, "test_class": args.test_class, 
        "test_method": args.test_method, "main_class_args": args.main_class_args, "dependencies": args.dependencies, "framework_models": args.framework_models
    }

if __name__ == "__main__":
   main()