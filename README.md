# transbyte

**Note**: The project is still WIP, so not all of your functions can be correctly encoded, or encoded at all.

**transbyte** is a tool for encoding JVM Bytecode to CIRCUIT-SAT.
It takes `.java` and `.class` files with some parameters and gives you an encoding in
ascii [AIGER](https://fmv.jku.at/aiger/FORMAT.aiger) or in [DIMACS](https://logic.pdmi.ras.ru/~basolver/dimacs.html) format.

## Usage

Use `.jar` standalone file from releases (or build one by yourself) with the next command to get usage info
```bash
> java -jar transbyte.jar --help
Usage: transbyte [OPTIONS] files...

Options:
  --start-class TEXT      Class name where to find start method
  --method TEXT           Name of the method to start translation with. If
                          class has only one method, this method will be taken
  --array-sizes INT       Array sizes for input method, separated by ','
  -o, --output TEXT       Filename for output
  -f, --format [aag|cnf]
  -d, --debug             Turn on debug info
  -h, --help              Show this message and exit

Arguments:
  files  All classes for the translator. You can also pass .java files, and
         transbyte will try to compile them using system Java compiler
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
> java -jar transbyte.jar Sum.class --start-class Sum --method sum
```

You can also pass `Sum.java` file directly, and `transbyte`
will try to compile it using system Java compiler.

If you have an array as input in your function, you should use parameter `--array-sizes`
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
> java -jar transbyte.jar BubbleSort.java --start-class BubbleSort --method bubbleSort --array-sizes 5
```

Multiple array sizes can be passed by separating sizes with `,` (in the appropriate order)
```java
class SumLength {
    public static int sumLength(int[] a, int[] b) {
        return a.length + b.length;
    }
}
```
```bash
> java -jar transbyte.jar SumLength.java --start-class SumLength --method sumLength --array-sizes 5,10
```

## Encoding
You can encode your function by following a few steps.

1. Suppose you have a `C.java` file with class `C` inside. Compile it with `javac`, or pass the file directly to `transbyte`.
2. Now you have encoding in `.aag` (ascii [AIGER](https://fmv.jku.at/aiger/FORMAT.aiger)) format
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
