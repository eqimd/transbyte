cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/LFSR/LFSR.class -sc LFSR -m get_lfsr:([Z)[Z --output tests/scripts/debug/lfsr_java.aag --array-sizes 19" &&
cd tests/scripts/ &&
./aag2cnf.py -n debug/lfsr_java.aag -o debug/lfsr_java.cnf > /dev/null &&
cp ../examples/LFSR/lfsr_reference_transalg.cnf debug/ &&
echo 'Testing equivalence of LFSR...' &&
./check_encodings.py debug/lfsr_java.cnf debug/lfsr_reference_transalg.cnf 19 128