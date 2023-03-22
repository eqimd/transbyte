# TODO now selection sort tests with transalg's selection sort, because transalg's insertion sort is wrong
cd $(dirname -- $(readlink -f $0)) &&
rm -rf debug &&
mkdir debug &&
cd ../../ &&
./gradlew run --args="-c tests/examples/Sorts/Sorts.class -sc Sorts -m pancakeSortBytesLikeTransalg:([B)[B --output tests/scripts/debug/pancake_liketr_5_8.aag --array-sizes 5" &&
cd tests/scripts/ &&
./aigtoaig debug/pancake_liketr_5_8.aag debug/pancake_liketr_5_8.aig &&
cp ../examples/Sorts/sel_transalg_5_8.aig debug/ &&
./abc -q "miter debug/pancake_liketr_5_8.aig debug/sel_transalg_5_8.aig ; fraig ; write_cnf debug/pancake_5_8_miter.cnf" &&
echo 'Testing equivalence of pancake sort on size 5x8...' &&
./kissat -q debug/pancake_5_8_miter.cnf | grep "UNSATISFIABLE"
