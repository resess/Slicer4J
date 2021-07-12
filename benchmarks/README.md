
This document describes the structure of Slicer4J's benchmarks, and how to run them.

## Table of Contents
1. [Benchmarks structure](#Benchmarks-structure)
2. [Running the Benchmarks](#Running-the-Benchmarks)

---
---

## Benchmarks structure
  There are 11 benchmarks: 3 borrowed from JavaSlicer, 5 we created to highlight Slicer4J's strengths, and 3 real programs from the Defects4J dataset. 


### JavaSlicer benchmarks


<table class="tg">
    <thead>
        <tr>
            <th class="tg-73oq">Benchmark</th>
            <th class="tg-73oq">Folder</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="tg-73oq">Intra-procedural</td>
            <td class="tg-73oq">javaslicer-bench1-intra-procedural</td>
        </tr>
        <tr>
            <td class="tg-73oq">Inter-procedural</td>
            <td class="tg-73oq">javaslicer-bench2-inter-procedural</td>
        </tr>
        <tr>
            <td class="tg-73oq">Exceptions</td>
            <td class="tg-73oq">javaslicer-bench3-exceptions</td>
        </tr>
    </tbody>
</table>

---

### Slicer4J benchmarks

<table>
    <thead>
        <tr>
            <th class="tg-73oq">Benchmark</th>
            <th class="tg-73oq">Folder</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td class="tg-73oq">Multiple threads</td>
            <td class="tg-73oq">slicer4j-bench1-multiple-threads</td>
        </tr>
        <tr>
            <td class="tg-73oq">Native methods</td>
            <td class="tg-73oq">slicer4j-bench2-native-framework</td>
        </tr>
        <tr>
            <td class="tg-73oq">Java 9 constructs</td>
            <td class="tg-73oq">slicer4j-bench3-java-9-constructs</td>
        </tr>
        <tr>
            <td class="tg-73oq">Instrumentation classes</td>
            <td class="tg-73oq">slicer4j-bench4-instrumentation-classes</td>
        </tr>
        <tr>
            <td class="tg-73oq">Static Constructor</td>
            <td class="tg-73oq">slicer4j-bench5-static-constructor</td>
        </tr>
    </tbody>
</table>

---

### Defects4J Programs

<table class="tg">
<thead>
    <tr>
        <th class="tg-73oq">Benchmark</th>
        <th class="tg-73oq">Folder</th>
    </tr>
</thead>
<tbody>
    <tr>
        <td class="tg-73oq">JacksonCore: bug #4</td>
        <td class="tg-73oq">JacksonCore_4b</td>
    </tr>
    <tr>
        <td class="tg-73oq">JacksonDatabind: bug #3</td>
        <td class="tg-73oq">JacksonDatabind_3b</td>
    </tr>
    <tr>
        <td class="tg-73oq">Gson: bug #4</td>
        <td class="tg-73oq">Gson_4b</td>
    </tr>
</tbody>
</table>

---
---

## Running the Benchmarks

Run the script <code>run_benchmarks.py</code> using python3: <code>python3 run_benchmarks.py</code> <br>
The script creates a results folder, runs all benchmarks, and places the traces and slices in the results folder.
