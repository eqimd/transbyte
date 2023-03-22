# transbyte

**Note**: The project is still WIP, so not all of your functions can be correctly encoded, or encoded at all.

**transbyte** is a tool for encoding JVM Bytecode to CIRCUIT-SAT.
It takes `.class` file with some parameters and gives you an encoding in
ascii [AIGER](https://fmv.jku.at/aiger/FORMAT.aiger) format.

## Usage

Use `.jar` standalone file from releases (or build one by yourself) with the next command to get usage info
```bash
> java -jar transbyte.jar
Value for option --classes should be always provided in command line.
Usage: transbyte options_list
Options: 
    --debug, -d [false] -> Turn on debug info 
    --classes, -c -> All paths to classes for the translator (always required) { String }
    --start-class, -sc -> Class name where to find start method (always required) { String }
    --method, -m [main] -> Name of the method to start translation with { String }
    --output, -o -> Filename for output { String }
    --array-sizes, -asz -> Array sizes for input in method { Int }
    --help, -h -> Usage info 
```

For example, if you have the next function
```java
class Sum {
    public static int sum(int a, int b) {
        return a + b;
    }
}
```
in your `Sum.class` file, you can pass it to `transbyte` using the next command
```bash
> java -jar transbyte.jar -c Sum.class -sc Sum -m sum:(II)I
```

If you have an array as input in your function, you should use parameter `--array-sizes` (or `-asz`)
to pass input array size (otherwise, `transbyte` will give an error).
```java
class BubbleSort {
    public static int[] bubbleSort(int[] sortArr) {
        for (int i = 0; i < sortArr.length - 1; i++) {
            for (int j = 0; j < sortArr.length - i - 1; j++) {
                if (sortArr[j + 1] < sortArr[j]) {
                    int swap = sortArr[j];
                    sortArr[j] = sortArr[j + 1];
                    sortArr[j + 1] = swap;
                }
            }
        }

        return sortArr;
    }
}
```
```bash
> java -jar transbyte.jar -c BubbleSort.class -sc BubbleSort -m bubbleSort:([I)[I -asz 5
```

Multiple array sizes can be passed using multiple `-asz` parameters (in the appropriate order)
```java
class SumLength {
    public static int sumLength(int[] a, int[] b) {
        return a.length + b.length;
    }
}
```
```bash
> java -jar transbyte.jar -c SumLength.class -sc SumLength -m sumLength:([I[I)I -asz 5 -asz 10
```

## Encoding
You can encode your function by following a few steps.

1. Suppose you have a `C.java` file with class `C` inside. Compile it with `javac`. Now you have a class file `C.class`
2. Pass it to `transbyte` with the necessary parameters. Now you have encoding in `.aag` (ascii [AIGER](https://fmv.jku.at/aiger/FORMAT.aiger)) format
   * You can convert it to binary `.aig` format (which is used in various tools) using [aigtoaig](https://github.com/arminbiere/aiger) tool

## Equivalence checking and minimization
If you have encodings of two functions (with same input and output sizes), 
you can check whether these two functions are equivalent or not.

You need two tools: [abc](https://github.com/berkeley-abc/abc) for miter creation, and your favourite SAT-solver (for example, [kissat](https://github.com/arminbiere/kissat)).

1. Create a miter and dump it to DIMACS-CNF with the command `./abc -q "miter enc1.aig enc2.aig ; write_cnf miter.cnf"`
2. Pass it to SAT-solver (for kissat, `./kissat miter.cnf`)
    * If the solver says UNSAT, it means that it could not find such an input that two encodings produce different outputs, so the original functions are equivalent
    * If the solver says SAT, it means that it found such an input where two encodings outputs differ (the solver also gives you the input), so the original functions are not equivalent

You can also minimize your encodings (or miter) with the `abc` tool, for example with `fraig` command
```bash
> ./abc -q "read enc1.aig ; fraig ; write enc1_minimized.aig"
> ./abc -q "read enc2.aig ; fraig ; write enc2_minimized.aig"
> ./abc -q "miter enc1_minimized.aig enc2_minimized.aig ; fraig ; write_cnf miter_minimized.cnf"
```

## License
`transbyte` is under the MIT license. See the [LICENSE.md](LICENSE.md) file for details.