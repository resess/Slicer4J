# Slicer4J


<img align="right" src="img/slicer4j_logo.png" alt="drawing" width="250"/>

This repository hosts Slicer4J, an accurate, low-overhead dynamic slicer for Java programs. 
Slicer4J automatically generates a backward dynamic slice from a user selected executed statement and variables used in the statement (slicing criterion). 
Slicer4J relies on soot which currently supports instrumenting programs compiled with up to Java 9. 
Contributions to this repo are most welcome!




## Table of Contents
1. [Pre-requisites](#pre-requisites)
2. [Building The Tool](#Building-The-Tool)
3. [Using The Tool](#Using-The-Tool)
    1. [Instrumenting](#Instrumenting)
    2. [Running apps](#Running-apps)
    3. [Generating ICDG](#Generating-icdg)
    4. [Slicing](#Slicing)

---


## Pre-requisites

* Install python3

    * Linux: https://docs.python-guide.org/starting/install3/linux/
    * Mac: https://docs.python-guide.org/starting/install3/osx/
    * Windows: https://docs.python.org/3/using/windows.html

* Clone the dynamic slicing core: https://github.com/resess/DynamicSlicingCore

---

## Building The Tool

Build and install the dynamic slicing core, go to the core's repo: (https://github.com/resess/DynamicSlicingCore)

```
cd core/
mvn -Dmaven.test.skip=true clean install
cd -
```


Build Slicer4J, go back to Slicer4J's repo
```
cd Slicer4J/
mvn -Dmaven.test.skip=true clean install
cd -
```

---

## Using The Tool


Display the command line options using:
```
java -cp "Mandoline/target/mandoline-jar-with-dependencies.jar:Mandoline/target/lib/*" ca.ubc.ece.resess.slicer.dynamic.slicer4j.Slicer -h
```
A simpler method to use Slicer4J is by using the wrapper python script: `scripts/slicer4j.py`

You can list the script options using: `python3 slicer4j.py -h`

---

## Slicer4J Mandatory Options: 


<table class="tg">
<thead>
  <tr>
    <th class="tg-73oq">Option<br></th>
    <th class="tg-73oq">Description</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td class="tg-73oq">-h</td>
    <td class="tg-73oq">show help message and exit</td>
  </tr>
  <tr>
    <td class="tg-73oq">-j</td>
    <td class="tg-73oq">Path to jar file</td>
  </tr>
  <tr>
    <td class="tg-73oq">-o</td>
    <td class="tg-73oq">Output folder</td>
  </tr>
  <tr>
    <td class="tg-73oq">-b</td>
    <td class="tg-73oq">line to slice backward from, in the form of FileName:LineNumber </td>
  </tr>
</tbody>
</table>

<br>
<br>

## Slicer4J Optional Options: 


<table class="tg">
<thead>
  <tr>
    <th class="tg-73oq">Option<br></th>
    <th class="tg-73oq">Description</th>
  </tr>
</thead>
<tbody>
  <tr>
    <td class="tg-73oq">-m</td>
    <td class="tg-73oq">Main class to run with arguments, in the form of "FileName Arguments"</td>
  </tr>
  <tr>
    <td class="tg-73oq">-tc</td>
    <td class="tg-73oq">Test class name to run, if this is provided, -tm must also be provided</td>
  </tr>
  <tr>
    <td class="tg-73oq">-tm</td>
    <td class="tg-73oq">Test method to run</td>
  </tr>
  <tr>
    <td class="tg-73oq">-dep</td>
    <td class="tg-73oq">Directory to folder containing JAR dependencies, if any</td>
  </tr>
  <tr>
    <td class="tg-0lax">-mod</td>
    <td class="tg-0lax">Folder containing user-defined method models</td>
  </tr>
  <tr>
    <td class="tg-0lax">-d</td>
    <td class="tg-0lax">Slice with data-flow dependencies only</td>
  </tr>
  <tr>
    <td class="tg-0lax">-c</td>
    <td class="tg-0lax">Slice with control dependencies only</td>
  </tr>
</tbody>
</table>

<br>
<br>

## User-defined method models: 

The following is an example for defining your own  method models. 

For the methods in this class:
```Java
package com.myproject;
class MyClass extends MyOtherClass{
    String field;
    public MyClass put(String val){
        this.field = val;
    }
    public String get(){
        return this.field;
    }
}
```

create an XML file named "com.myproject.MyClass.xml" and place it in a folder containing your method models, this is the folder we pass to Slicer4J using the `-mod` option

For example, here's the model for the above class
```xml
<?xml version="1.0" ?>
<summary fileFormatVersion="101">
<hierarchy superClass="com.myproject.MyOtherClass" />
  <methods>
    <method id="com.myproject.MyClass put(java.lang.String)">
      <flows>
        <flow>
          <from sourceSinkType="Parameter" ParameterIndex="0" />
          <to sourceSinkType="Field" AccessPath="[com.myproject.MyClass: java.lang.String field]"
          	AccessPathTypes="java.lang.String" />
        </flow>
        <flow>
          <from sourceSinkType="Field" />
          <to sourceSinkType="Return" />
        </flow>
      </flows>
    </method>
    <method id="java.lang.String get()">
      <flows>
        <flow>
          <from sourceSinkType="Field" AccessPath="[java.nio.CharBuffer: char[] buffer]"
          	AccessPathTypes="[char[]]" />
           <to sourceSinkType="Return" />
        </flow>
      </flows>
    </method>
</summary>
```

The `id` of each method is the method signature. Each method has flows `from` parameters, the receiver, and their fields, `to` other parameters, the receiver, their fields, and the return.


Each flow is specified with it `sourceSinkType` as `Parameter`, `Field`, or `Return`.
`Parameter` is used for parameters. `Field` is used for the receiver or fields of the receiver. `Return` is for the method return.
For parameters, we also need `ParameterIndex` to specify which parameter (first, second, etc.).
For fields, we specify the signature of the field in `AccessPath` and its type in `AccessPathTypes`.




# Contact

If you experience any issues, please submit an issue or contact us at khaledea@ece.ubc.ca
