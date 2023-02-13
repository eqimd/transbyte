cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/A5_1/A5_1.class -sc A5_1 -m generate:([Z[Z[Z)[Z --output tests/scripts/debug/a51_java.aag --array-sizes 19 --array-sizes 22 --array-sizes 23" &&
cd tests/scripts/ &&
./aag2cnf.py -n debug/a51_java.aag -o debug/a51_java.cnf > /dev/null &&
cp ../examples/A5_1/a51_reference_transalg.cnf debug/ &&
echo 'Testing equivalence of A5/1...' &&
./check_encodings.py debug/a51_java.cnf debug/a51_reference_transalg.cnf 64 128