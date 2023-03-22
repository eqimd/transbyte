#!/bin/python3

import os
import sys

from math import copysign

INPUT_SZ = 64
OUTPUT_SZ = 32

OPERATION = None

enc_java = None
enc_java_with_input = 'debug/enc_with_input.cnf'
enc_java_solved = 'debug/enc_solved'


def add_input_to_files(inp):
	f_java = open(enc_java_with_input, 'w')

	with open(enc_java, 'r') as f:
		for line in f:
			f_java.write(line)

	for i in range(0, INPUT_SZ):
		bit = (inp >> i) & 1
		minus = '' if bit else '-'

		f_java.write(f"{minus}{i+1} 0\n")


def lastNlines(fname, N):
	with open(fname) as file:
		ret = []
		for line in file.readlines()[::-1]:
			if line[0] != 'd':
				ret.append(int(line.split()[0]))
				
		ret.sort(key=lambda x: abs(x))
		ret = ret[-1:-(OUTPUT_SZ + 1):-1]
			
		return ret


def check_bits(num_begin, num_end):
	for num1 in range(num_begin, num_end):
		for num2 in range(num_begin, num_end):
			add_input_to_files((num1 << 32) + num2)

			print(f'Checking equality on inputs {num1} and {num2}...', end=' ')
			os.system(f'./kissat -q --no-binary --relaxed {enc_java_with_input} {enc_java_solved} > /dev/null')

			def sign_bit(x): return int(copysign(1, int(x)) + 1) // 2

			bits_java = [
				sign_bit(rec)
				for rec in lastNlines(enc_java_solved, OUTPUT_SZ)
			][::-1]
			
			ans_num = sum(
				[bit<<i for i, bit in enumerate(bits_java)]
			)

			res = None
			if OPERATION == 'mult':
				res = (num1 * num2) % (2**OUTPUT_SZ)
			elif OPERATION == 'sum':
				res = (num1 + num2) % (2**OUTPUT_SZ)

			if res != ans_num:
				print(f'Results are not equal!', file=sys.stderr)
				print(f'Inputs: {num1} {num2}', file=sys.stderr)
				print(f'Real result: {res}', file=sys.stderr)
				print(f'Java encoding result: {ans_num}', file=sys.stderr)
				sys.exit(1)

			print('Equal.')



if __name__ == '__main__':
	enc_java = sys.argv[1]
	OPERATION = sys.argv[2]

	num = 1234
	check_bits(num, num + 10)

	num = 57745
	check_bits(num, num + 10)

	num = 5777744
	check_bits(num, num + 10)
