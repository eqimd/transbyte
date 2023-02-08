rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/Operations/Operations.class -s Operations -m multiply:(II)I --output tests/scripts/debug/mult_java.aag" &&
cd tests/scripts/ &&
./aag2cnf.py -n debug/mult_java.aag -o debug/mult_java.cnf > /dev/null &&
echo 'Testing Multiply...' &&
./check_operations.py debug/mult_java.cnf mult