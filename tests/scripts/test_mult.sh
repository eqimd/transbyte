cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="tests/examples/Operations/Operations.class --start-class Operations --method multiply --output tests/scripts/debug/mult_java.aag" &&
cd tests/scripts/ &&
./aag2cnf.py -n debug/mult_java.aag -o debug/mult_java.cnf > /dev/null &&
echo 'Testing Multiply...' &&
./check_operations.py debug/mult_java.cnf mult