cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/LFSR/LFSR.class -sc LFSR -m get_lfsr:([Z)[Z --output tests/scripts/debug/lfsr_java.aag --array-sizes 19" &&
cd tests/scripts/ &&
./aigtoaig debug/lfsr_java.aag debug/lfsr_java.aig &&
cp ../examples/LFSR/lfsr_reference_transalg.aig debug/ &&
./abc -q "read debug/lfsr_java.aig ; fraig ; miter debug/lfsr_reference_transalg.aig ; fraig ; write_cnf debug/lfsr_miter.cnf" &&
echo 'Testing equivalence of LFSR...' &&
./kissat -q debug/lfsr_miter.cnf | grep "UNSATISFIABLE"
