cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="tests/examples/Sorts/Sorts.class --start-class Sorts --method selectionSortBytes --output tests/scripts/debug/sel_5_8.aag --array-sizes 5" &&
cd tests/scripts/ &&
./aigtoaig debug/sel_5_8.aag debug/sel_5_8.aig &&
cp ../examples/Sorts/sel_transalg_5_8.aig debug/ &&
./abc -q "miter debug/sel_5_8.aig debug/sel_transalg_5_8.aig ; fraig ; write_cnf debug/sel_5_8_miter.cnf" &&
echo 'Testing equivalence of selection sort on size 5x8...' &&
./kissat -q debug/sel_5_8_miter.cnf | grep "UNSATISFIABLE"
