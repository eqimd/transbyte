cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/Sorts/Sorts.class -sc Sorts -m insertionSortBytes:([B)[B --output tests/scripts/debug/ins_5_8.aag --array-sizes 5" &&
cd tests/scripts/ &&
./aigtoaig debug/ins_5_8.aag debug/ins_5_8.aig &&
cp ../examples/Sorts/ins_transalg_5_8.aig debug/ &&
./abc -q "miter debug/ins_5_8.aig debug/ins_transalg_5_8.aig ; fraig ; write_cnf debug/ins_5_8_miter.cnf" &&
echo 'Testing equivalence of insertion sort on size 5x8...' &&
./kissat -q debug/ins_5_8_miter.cnf | grep "UNSATISFIABLE"
