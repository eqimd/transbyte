cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/A5_1/A5_1.class -sc A5_1 -m generate:([Z[Z[Z)[Z --output tests/scripts/debug/a51_java.aag --array-sizes 19 --array-sizes 22 --array-sizes 23" &&
cd tests/scripts/ &&
./aigtoaig debug/a51_java.aag debug/a51_java.aig &&
cp ../examples/A5_1/a51_reference_transalg.aig debug/ &&
./abc -q "read debug/a51_java.aig ; fraig ; miter debug/a51_reference_transalg.aig ; fraig ; write_cnf debug/a51_miter.cnf" &&
echo 'Testing equivalence of A5/1...' &&
./kissat -q debug/a51_miter.cnf | grep "UNSATISFIABLE"