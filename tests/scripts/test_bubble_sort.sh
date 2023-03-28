cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="tests/examples/Sorts/Sorts.class --start-class Sorts --method bubbleSortBytes --output tests/scripts/debug/bub_5_8.aag --array-sizes 5" &&
cd tests/scripts/ &&
./aigtoaig debug/bub_5_8.aag debug/bub_5_8.aig &&
cp ../examples/Sorts/bub_transalg_5_8.aig debug/ &&
./abc -q "miter debug/bub_5_8.aig debug/bub_transalg_5_8.aig ; fraig ; write_cnf debug/bub_5_8_miter.cnf" &&
echo 'Testing equivalence of bubble sort on size 5x8...' &&
./kissat -q debug/bub_5_8_miter.cnf | grep "UNSATISFIABLE"
