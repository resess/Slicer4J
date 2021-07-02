from os import name
import sys

if __name__=='__main__':
    trace = sys.argv[1]
    trace_list = list()
    with open(trace, 'r') as f:
        for line in f:
            trace_list.append(line.rstrip())
    print(f"Length of trace in bytecode is {len(trace_list)}")
    all_locations = set()
    full_line = list()
    prev_src_location = None
    for trace_line in trace_list:
        if ":LINENO:" in trace_line:
            src_location = trace_line.split(":LINENO:")[1].split(":PRED:")[0]
            if src_location != prev_src_location:
                all_locations.add(src_location)
                full_line.append(trace_line)
        prev_src_location = src_location

    print(f"Length of trace in src code is {len(all_locations)}")
    for l in full_line:
        print(l)
