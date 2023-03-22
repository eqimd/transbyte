#!/bin/python3

import os
import sys

from math import copysign

INPUT_SZ = None
OUTPUT_SZ = None

enc_java = None
enc_java_with_input = 'debug/enc_java_with_input.cnf'
enc_java_solved = 'debug/enc_java_solved'

enc_reference = None
enc_reference_with_input = 'debug/enc_reference_with_input.cnf'
enc_reference_solved = 'debug/enc_reference_solved'


def add_input_to_files(inp):
    f_java = open(enc_java_with_input, 'w')
    f_reference = open(enc_reference_with_input, 'w')

    with open(enc_java, 'r') as f:
        line = f.readline()
        while line[0] == 'p' or line[0] == 'c':
            f_java.write(line)
            line = f.readline()

        for i in range(0, INPUT_SZ):
            bit = (inp >> i) & 1
            minus = '' if bit else '-'

            f_java.write(f"{minus}{i+1} 0\n")

        while line != '':
            f_java.write(line)
            line = f.readline()

    with open(enc_reference, 'r') as f:
        line = f.readline()
        while line[0] == 'p' or line[0] == 'c':
            f_reference.write(line)
            line = f.readline()

        for i in range(0, INPUT_SZ):
            bit = (inp >> i) & 1
            minus = '' if bit else '-'

            f_reference.write(f"{minus}{i+1} 0\n")

        while line != '':
            f_reference.write(line)
            line = f.readline()


def lastNlines(fname, N):
	with open(fname) as file:
		cnt = 0
		ret = []
		for line in file.readlines()[::-1]:
			if line[0] != 'd':
				ret.append(line)
				cnt += 1
				
			if cnt == OUTPUT_SZ:
				break
			
		return ret


def check_bits(num_begin, num_end):
    for inp in range(num_begin, num_end):
        add_input_to_files(inp)

        print(f'Checking equality on input {inp}...', end=' ')

        os.system(f'./kissat -q --no-binary --relaxed {enc_java_with_input} {enc_java_solved} > /dev/null')
        os.system(f'./kissat -q --no-binary --relaxed {enc_reference_with_input} {enc_reference_solved} > /dev/null')

        def sign_bit(x): return int(copysign(1, int(x)) + 1) // 2

        bits_java = [
            sign_bit(rec.split()[0])
            for rec in lastNlines(enc_java_solved, OUTPUT_SZ)
        ]
        bits_reference = [
            sign_bit(rec.split()[0])
            for rec in lastNlines(enc_reference_solved, OUTPUT_SZ)
        ]

        if bits_java != bits_reference:
            print(f'Encodings are not equal!', file=sys.stderr)
            print(f'Input: {inp}', file=sys.stderr)
            print(f'Java encoding output: {bits_java}', file=sys.stderr)
            print(
                f'Reference encoding output: {bits_reference}', file=sys.stderr)
            sys.exit(1)

        print('Equal.')


if __name__ == '__main__':
    enc_java = sys.argv[1]
    enc_reference = sys.argv[2]

    INPUT_SZ = int(sys.argv[3])
    OUTPUT_SZ = int(sys.argv[4])

    check_bits(0, 100)

    mid = 2**(INPUT_SZ - 1)
    check_bits(mid - 50, mid + 50)

    end = (2**INPUT_SZ) - 1
    check_bits(end - 100, end)
