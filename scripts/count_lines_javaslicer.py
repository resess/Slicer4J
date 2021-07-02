from os import name
import sys

if __name__=='__main__':
    trace = sys.argv[1]
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
    print(f"Length of trace in bytecode is {len(trace_list)}")
    all_locations = list()
    full_line = list()
    prev_src_location = None
    for trace_line in trace_list:
        if ":" in trace_line:
            # com.fasterxml.jackson.core.util.TextBuffer.unshare:639 ILOAD 2
            src_location = trace_line.split(" ")[0]
            if any([x for x in ["The", "sun.", "org.junit", "junit.framework", "java.", "com.sun.", "junit."] if src_location.startswith(x)]):
                continue
            if src_location != prev_src_location:
                all_locations.append(src_location)
            full_line.append(trace_line)
        prev_src_location = src_location

    print(f"Length of trace in src code is {len(all_locations)}")
    for l in full_line:
        print(l)
