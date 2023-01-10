package instruction_parser

import bit_scheduler.BitScheduler
import boolean_logic.BooleanFormula
import constants.BooleanSystem
import constants.Constants.INT_BITS
import extension.minus
import extension.plus
import extension.times
import extension.xor
import parsed_types.data.VariableSat

object InstructionParser {
    @JvmStatic
    fun parseSum(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
        bitScheduler: BitScheduler
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val varSize = a.bitsArray.size

        if (a.constant != null && b.constant != null) {
            val constC = a.constant + b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<BooleanFormula.Equality>().toMutableList()

        // TODO if we don't want to consider overflowing the size should be varSize + 1
        // c = a + b
        val (cPrim, _) = VariableSat.Primitive.create(
            size = varSize,
            bitScheduler = bitScheduler
        )
        val c = cPrim.bitsArray

        val (xorBit, xorSystem) = parseXorBits(a.bitsArray[0], b.bitsArray[0], bitScheduler)
        system.addAll(xorSystem)

        val c0 = BooleanFormula.Equality(
            c[0],
            xorBit
        )
        system.add(c0)

        val pArr = bitScheduler.getAndShift(varSize)
        var pPrev = pArr[0]
        val p0 = BooleanFormula.Equality(
            pPrev,
            a.bitsArray[0],
            b.bitsArray[0]
        )
        system.add(p0)

        for (i in 1 until varSize) {
            val (majBit, majSys) = parseMajorityBits(a.bitsArray[i], b.bitsArray[i], pPrev, bitScheduler)
            system.addAll(majSys)

            val (xorInnerAiBi, xorInnerSys) = parseXorBits(a.bitsArray[i], b.bitsArray[i], bitScheduler)
            system.addAll(xorInnerSys)

            val (xorWithPrev, xorPrevSys) = parseXorBits(xorInnerAiBi, pPrev, bitScheduler)
            system.addAll(xorPrevSys)

            pPrev = pArr[i]
            val pI = BooleanFormula.Equality(
                pPrev,
                majBit
            )
            val cI = BooleanFormula.Equality(
                c[i],
                xorWithPrev
            )
            system.add(pI)
            system.add(cI)
        }

        // TODO no need if we do not consider overflowing
//            val cLast = Equality(
//                c[c.size - 1],
//                pPrev
//            )
//            system.add(cLast)

        return Pair(cPrim, system)
    }

    @JvmStatic
    fun parseMajorityBits(
        x: BooleanFormula.Variable.Bit,
        y: BooleanFormula.Variable.Bit,
        z: BooleanFormula.Variable.Bit,
        bitScheduler: BitScheduler
    ): Pair<BooleanFormula.Variable.Bit, BooleanSystem> {
        /*
         * Majority(x, y, z) = (x and y) or (x and z) or (y and z)
         * == not(not(x and y) and not(x and z) and not (y and z))
         */

        val majBits = bitScheduler.getAndShift(4)
        val bitXY = majBits[0]
        val bitXZ = majBits[1]
        val bitYZ = majBits[2]
        val allBit = majBits[3]

        val bitXyXzNeg = bitScheduler.getAndShift(1).first()

        val system = listOf(
            BooleanFormula.Equality(bitXY, x, y),
            BooleanFormula.Equality(bitXZ, x, z),
            BooleanFormula.Equality(bitYZ, y, z),
            BooleanFormula.Equality(bitXyXzNeg, bitXY.negated(), bitXZ.negated()),
            BooleanFormula.Equality(
                allBit,
                bitXyXzNeg,
                bitYZ.negated()
            )
        )

        return Pair(allBit.negated(), system)
    }

    @JvmStatic
    fun parseDisjunctionBits(
        x: BooleanFormula.Variable.Bit,
        y: BooleanFormula.Variable.Bit,
        bitScheduler: BitScheduler
    ): Pair<BooleanFormula.Variable.Bit, BooleanSystem> {
        /*
         * X or Y == not(not X and not Y)
         * == not A
         * One new bit: A
         */

        val disjBit = bitScheduler.getAndShift(1).first() // A

        val system = listOf(
            BooleanFormula.Equality(
                disjBit,
                x.negated(),
                y.negated()
            )
        )

        return Pair(disjBit.negated(), system)
    }

    @JvmStatic
    fun parseEqualityBits(
        x: BooleanFormula.Variable.Bit,
        y: BooleanFormula.Variable.Bit,
        bitScheduler: BitScheduler
    ): Pair<BooleanFormula.Variable.Bit, BooleanSystem> {
        /*
         * Equality(x, y) = (x and y) or (not x and not y)
         */

        val conjBits = bitScheduler.getAndShift(2)
        val lhsBit = conjBits[0]
        val rhsBit = conjBits[1]

        val system = listOf(
            BooleanFormula.Equality(lhsBit, x, y),
            BooleanFormula.Equality(rhsBit, x.negated(), y.negated())
        ).toMutableList()

        val (disjBit, disjSys) = parseDisjunctionBits(lhsBit, rhsBit, bitScheduler)
        system.addAll(disjSys)

        return Pair(disjBit, system)
    }

    @JvmStatic
    fun parseMultiply(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
        bitScheduler: BitScheduler
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val varSize = a.bitsArray.size

        if (a.constant != null && b.constant != null) {
            val constC = a.constant * b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<BooleanFormula.Equality>().toMutableList()

        // TODO if we don't want to consider overflowing the size should be 2 * varSize
        // c = a*b
        val (cPrim, _) = VariableSat.Primitive.create(
            size = varSize,
            bitScheduler = bitScheduler
        )
        val c = cPrim.bitsArray

        val temp = bitScheduler.getAndShift(varSize * varSize)
        val tempMult = Array(varSize) { i ->
            Array(varSize) { j -> temp[i * varSize + j] }
        }

        val sumRes = bitScheduler.getAndShift(varSize * varSize)
        val sumResMult = Array(varSize) { i ->
            Array(varSize) { j -> sumRes[i * varSize + j] }
        }

        val carry = bitScheduler.getAndShift(varSize * varSize)
        val carryMult = Array(varSize) { i ->
            Array(varSize) { j -> carry[i * varSize + j] }
        }

        for (i in 0 until varSize) {
            for (j in 0 until varSize) {
                system.add(
                    BooleanFormula.Equality(
                        tempMult[i][j],
                        a.bitsArray[i],
                        b.bitsArray[j]
                    )
                )
            }
        }

        for (i in 0 until varSize - 1) {
            val (xor, xorSys) = parseXorBits(tempMult[0][i + 1], tempMult[i + 1][0], bitScheduler)
            system.addAll(xorSys)
            system.add(
                BooleanFormula.Equality(
                    sumResMult[0][i],
                    xor
                )
            )

            system.add(
                BooleanFormula.Equality(
                    carryMult[0][i],
                    tempMult[0][i + 1],
                    tempMult[i + 1][0]
                )
            )
        }

        for (i in 0 until varSize - 1) {
            for (j in 0 until varSize - 1) {
                val (xorInnerBit, xorInnerSys) = parseXorBits(
                    sumResMult[i][j + 1],
                    tempMult[j + 1][i + 1],
                    bitScheduler
                )
                system.addAll(xorInnerSys)

                val (xorOuterBit, xorOuterSys) = parseXorBits(
                    carryMult[i][j],
                    xorInnerBit,
                    bitScheduler
                )
                system.addAll(xorOuterSys)

                system.add(
                    BooleanFormula.Equality(sumResMult[i + 1][j], xorOuterBit)
                )

                val conjBits = bitScheduler.getAndShift(3)
                val conj1 = conjBits[0]
                val conj2 = conjBits[1]
                val conj3 = conjBits[2]
                system.addAll(
                    listOf(
                        BooleanFormula.Equality(conj1, tempMult[j + 1][i + 1], carryMult[i][j]),
                        BooleanFormula.Equality(conj2, tempMult[j + 1][i + 1], sumResMult[i][j + 1]),
                        BooleanFormula.Equality(conj3, carryMult[i][j], sumResMult[i][j + 1])
                    )
                )

                val (disjInnerBit, disjInnerSys) = parseDisjunctionBits(conj2, conj3, bitScheduler)
                system.addAll(disjInnerSys)

                val (disjOuterBit, disjOuterSys) = parseDisjunctionBits(conj1, disjInnerBit, bitScheduler)
                system.addAll(disjOuterSys)
                system.add(
                    BooleanFormula.Equality(
                        carryMult[i + 1][j],
                        disjOuterBit
                    )
                )
            }
        }

        system.add(
            BooleanFormula.Equality(c[0], tempMult[0][0])
        )

        for (i in 1 until varSize) {
            system.add(
                BooleanFormula.Equality(c[i], sumResMult[i - 1][0])
            )
        }

        // TODO no need if we do not consider overflowing
//            for (i in 0 until varSize - 1) {
//                system.add(
//                    Equality(c[i + varSize], sumResMult[varSize - 1][i])
//                )
//            }
//
//            system.add(
//                Equality(c[2 * varSize - 1], carryMult[varSize - 1][varSize - 2])
//            )

        return Pair(cPrim, system)
    }

    @JvmStatic
    fun parseSubtraction(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
        bitScheduler: BitScheduler
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        // a - b

        val varSize = a.bitsArray.size

        if (a.constant != null && b.constant != null) {
            val constC = a.constant - b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<BooleanFormula.Equality>().toMutableList()

        /*
         *  Logic is next:
         *  a - b = a + (2^N - b) = a + ([N bits of 1] - [b_N ... b_1] + 1) = a + ~b + 1
         */

        // bInverted = ~b
        val (bInverted, _) = VariableSat.Primitive.create(
            size = b.bitsArray.size,
            bitScheduler = bitScheduler
        )

        for (i in 0 until b.bitsArray.size) {
            system.add(
                BooleanFormula.Equality(
                    bInverted.bitsArray[i],
                    b.bitsArray[i].negated()
                )
            )
        }

        val (one, oneSystem) = parsePush(1, bitScheduler)
        system.addAll(oneSystem)

        val (bInvertedPlusOne, plusOneSystem) = parseSum(bInverted, one, bitScheduler)
        system.addAll(plusOneSystem)

        val (c, sumSystem) = parseSum(a, bInvertedPlusOne, bitScheduler)
        system.addAll(sumSystem)

        return Pair(c, system)
    }

    @JvmStatic
    fun parseXorBits(
        x: BooleanFormula.Variable.Bit,
        y: BooleanFormula.Variable.Bit,
        bitScheduler: BitScheduler
    ): Pair<BooleanFormula.Variable.Bit, BooleanSystem> {
        /*
         * X xor Y == (X and not Y) or (not X and Y)
         * == not (not (X and not Y) and not (not X and Y))
         * == not ( not A and not B)
         * == not C
         * Three new bits: A, B, C
         */

        val xorBits = bitScheduler.getAndShift(3)

        val xorLhsBit = xorBits[0] // A
        val xorLhsNegatedBit = xorLhsBit.negated() // not A

        val xorRhsBit = xorBits[1] // B
        val xorRhsNegatedBit = xorRhsBit.negated() // not B

        val xorFullBit = xorBits[2] // C
        val xorFullNegatedBit = xorFullBit.negated() // not C

        val system = listOf(
            BooleanFormula.Equality(
                xorLhsBit,
                x,
                y.negated()
            ),
            BooleanFormula.Equality(
                xorRhsBit,
                x.negated(),
                y
            ),
            BooleanFormula.Equality(
                xorFullBit,
                xorLhsNegatedBit.negated(),
                xorRhsNegatedBit.negated()
            )
        )

        return Pair(xorFullNegatedBit, system)
    }

    @JvmStatic
    fun parseXor(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
        bitScheduler: BitScheduler
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val varSize = a.bitsArray.size

        if (a.constant != null && b.constant != null) {
            val constC = a.constant xor b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<BooleanFormula.Equality>().toMutableList()

        val (c, _) = VariableSat.Primitive.create(
            size = varSize,
            bitScheduler = bitScheduler
        )
        val cArr = c.bitsArray

        for (i in 0 until a.bitsArray.size) {
            val (xor, xorSys) = parseXorBits(a.bitsArray[i], b.bitsArray[i], bitScheduler)
            system.addAll(xorSys)
            system.add(
                BooleanFormula.Equality(
                    cArr[i],
                    xor
                )
            )
        }

        return Pair(c, system)
    }

    @JvmStatic
    fun parsePush(value: Number, bitScheduler: BitScheduler): Pair<VariableSat.Primitive, BooleanSystem> {
        // TODO add varSize for longs

        return VariableSat.Primitive.create(
            size = INT_BITS,
            constant = value,
            bitScheduler = bitScheduler
        )
    }

    @JvmStatic
    fun parseLessCondition(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
        bitScheduler: BitScheduler
    ): Pair<BooleanFormula.Variable.Bit, BooleanSystem> {
        // Condition: a < b

        val system = emptyList<BooleanFormula.Equality>().toMutableList()
        // TODO maybe .constant.toInt() is enough
        if (a.constant != null && b.constant != null) {
            val bit = bitScheduler.getAndShift(1).first()
            system.add(
                BooleanFormula.Equality(
                    bit,
                    BooleanFormula.Variable.Constant.getByBoolean(a.constant.toLong() < b.constant.toLong())
                )
            )
            return Pair(bit, system)
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)

        system.add(
            BooleanFormula.Equality(
                systemBits.first(),
                b.bitsArray.first(),
                a.bitsArray.first().negated()
            )
        )

        // TODO works when a.bitsArray.size == b.bitsArray.size
        for (i in 1 until a.bitsArray.size) {
            val (eqBit, eqSys) = parseEqualityBits(b.bitsArray[i], a.bitsArray[i], bitScheduler)
            system.addAll(eqSys)
            val conjBits = bitScheduler.getAndShift(2)
            val conj1 = conjBits[0]
            val conj2 = conjBits[1]
            system.addAll(
                listOf(
                    BooleanFormula.Equality(conj1, b.bitsArray[i], a.bitsArray[i].negated()),
                    BooleanFormula.Equality(conj2, eqBit, systemBits[i - 1]),
                )
            )

            val (disjBit, disjSys) = parseDisjunctionBits(conj1, conj2, bitScheduler)
            system.addAll(disjSys)
            system.add(
                BooleanFormula.Equality(
                    systemBits[i],
                    disjBit
                )
            )
        }

        return Pair(systemBits.last(), system)
    }

    @JvmStatic
    fun parseLessOrEqualCondition(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
        bitScheduler: BitScheduler
    ): Pair<BooleanFormula.Variable.Bit, BooleanSystem> {
        // Condition: a <= b

        val system = emptyList<BooleanFormula.Equality>().toMutableList()
        // TODO maybe .constant.toInt() is enough
        if (a.constant != null && b.constant != null) {
            val bit = bitScheduler.getAndShift(1).first()
            system.add(
                BooleanFormula.Equality(
                    bit,
                    BooleanFormula.Variable.Constant.getByBoolean(a.constant.toLong() <= b.constant.toLong())
                )
            )
            return Pair(bit, system)
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)

        val conjAB = bitScheduler.getAndShift(1).first()
        system.add(
            BooleanFormula.Equality(conjAB, a.bitsArray.first(), b.bitsArray.first())
        )

        val (disjBitFirst, disjSysFirst) = parseDisjunctionBits(a.bitsArray.first().negated(), conjAB, bitScheduler)
        system.addAll(disjSysFirst)
        system.add(
            BooleanFormula.Equality(
                systemBits.first(),
                disjBitFirst
            )
        )

        // TODO works when a.bitsArray.size == b.bitsArray.size
        for (i in 1 until a.bitsArray.size) {
            val (eqBit, eqSys) = parseEqualityBits(b.bitsArray[i], a.bitsArray[i], bitScheduler)
            system.addAll(eqSys)
            val conjBits = bitScheduler.getAndShift(2)
            val conj1 = conjBits[0]
            val conj2 = conjBits[1]
            system.addAll(
                listOf(
                    BooleanFormula.Equality(conj1, b.bitsArray[i], a.bitsArray[i].negated()),
                    BooleanFormula.Equality(conj2, eqBit, systemBits[i - 1]),
                )
            )

            val (disjBit, disjSys) = parseDisjunctionBits(conj1, conj2, bitScheduler)
            system.addAll(disjSys)
            system.add(
                BooleanFormula.Equality(
                    systemBits[i],
                    disjBit
                )
            )
        }

        return Pair(systemBits.last(), system)
    }

    @JvmStatic
    fun parseGreaterThanZero(
        a: VariableSat.Primitive,
        bitScheduler: BitScheduler
    ): Pair<BooleanFormula.Variable.Bit, BooleanSystem> {

        // a > 0
        val system = emptyList<BooleanFormula.Equality>().toMutableList()
        if (a.constant != null) {
            val bit = bitScheduler.getAndShift(1).first()
            system.add(
                BooleanFormula.Equality(
                    bit,
                    BooleanFormula.Variable.Constant.getByBoolean(a.constant.toLong() > 0)
                )
            )
            return Pair(bit, system)
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)
        system.add(
            BooleanFormula.Equality(
                systemBits.first(),
                a.bitsArray.first()
            )
        )

        for (i in 1 until a.bitsArray.size) {
            val (disj, disjSys) = parseDisjunctionBits(a.bitsArray[i], systemBits[i - 1], bitScheduler)
            system.addAll(disjSys)
            system.add(
                BooleanFormula.Equality(
                    systemBits[i],
                    disj
                )
            )
        }

        return Pair(systemBits.last(), system)
    }
}
