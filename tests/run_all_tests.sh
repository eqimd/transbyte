cd $(dirname -- $(readlink -f $0)) &&
chmod +x configure.sh && ./configure.sh &&
cd scripts &&
./test_sum.sh &&
./test_mult.sh &&
./test_lfsr.sh &&
./test_a5_1.sh &&
./test_wolfram.sh &&
./test_selection_sort.sh &&
./test_insertion_sort.sh &&
./test_bubble_sort.sh &&
./test_pancake_sort.sh