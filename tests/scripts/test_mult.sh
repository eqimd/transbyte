cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="tests/examples/Operations/Operations.class --start-class Operations --method multiply --format cnf --output tests/scripts/debug/mult_java.cnf" &&
cd tests/scripts/ &&
echo 'Testing Multiply...' &&
./check_operations.py debug/mult_java.cnf mult