cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="tests/examples/Operations/Operations.class --start-class Operations --method sum --output tests/scripts/debug/sum_java.aag" &&
cd tests/scripts/ &&
./aag2cnf.py -n debug/sum_java.aag -o debug/sum_java.cnf > /dev/null &&
echo 'Testing Sum...' &&
./check_operations.py debug/sum_java.cnf sum