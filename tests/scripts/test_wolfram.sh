rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/WolframGenerator/WolframGenerator.class -s WolframGenerator -m generate:([Z[Z)[Z --output tests/scripts/debug/wolfram_java.aag --array-sizes 128" &&
cd tests/scripts/ &&
./aag2cnf.py -n debug/wolfram_java.aag -o debug/wolfram_java.cnf > /dev/null &&
cp ../examples/WolframGenerator/wolfram_reference_transalg.cnf debug/ &&
echo 'Testing equivalence of Wolfram...' &&
./check_encodings.py debug/wolfram_java.cnf debug/wolfram_reference_transalg.cnf 128 128