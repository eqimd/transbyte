cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="tests/examples/Operations/Operations.class --start-class Operations --method sum --format cnf --output tests/scripts/debug/sum_java.cnf" &&
cd tests/scripts &&
echo 'Testing Sum...' &&
./check_operations.py debug/sum_java.cnf sum