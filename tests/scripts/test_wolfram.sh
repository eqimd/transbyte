cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/WolframGenerator/WolframGenerator.class -sc WolframGenerator -m generate:([Z)[Z --output tests/scripts/debug/wolfram_java.aag --array-sizes 128" &&
cd tests/scripts/ &&
./aigtoaig debug/wolfram_java.aag debug/wolfram_java.aig &&
cp ../examples/WolframGenerator/wolfram_reference_transalg.aig debug/ &&
./abc -q "read debug/wolfram_java.aig ; fraig ; miter debug/wolfram_reference_transalg.aig ; fraig ; write_cnf debug/wolfram_miter.cnf" &&
echo 'Testing equivalence of Wolfram...' &&
./kissat -q debug/wolfram_miter.cnf | grep "UNSATISFIABLE"
