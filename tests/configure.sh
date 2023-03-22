cd $(dirname -- $(readlink -f $0)) &&
python3 -m pip install toposort &&
cd scripts &&
chmod +x aag2cnf.py &&
chmod +x check_encodings.py &&
chmod +x check_operations.py &&
chmod +x test_sum.sh &&
chmod +x test_mult.sh &&
chmod +x test_lfsr.sh &&
chmod +x test_a5_1.sh &&
chmod +x test_wolfram.sh &&
chmod +x test_selection_sort.sh &&
chmod +x test_insertion_sort.sh &&
chmod +x test_bubble_sort.sh &&
chmod +x test_pancake_sort.sh