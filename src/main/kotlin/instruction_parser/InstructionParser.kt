package instruction_parser

import bit_scheduler.BitScheduler
import boolean_logic.additional.Equality
import boolean_logic.additional.Maj
import boolean_logic.additional.Xor
import boolean_logic.base.BitValue
import boolean_logic.base.BitVariable
import boolean_logic.base.Conjunction
import boolean_logic.base.Disjunction
import boolean_logic.base.Negated
import constants.BooleanSystem
import constants.Constants.INT_BITS
import extension.minus
import extension.plus
import extension.times
import extension.xor
import parsed_types.data.Variable

object InstructionParser {
    @JvmStatic
    fun parseSum(
        a: Variable.BitsArrayWithNumber,
        b: Variable.BitsArrayWithNumber,
        varSize: Int = INT_BITS,
        bitScheduler: BitScheduler
    ): Pair<Variable.BitsArrayWithNumber, BooleanSystem> {

        if (a.constant != null && b.constant != null) {
            val constC = a.constant + b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = Variable.BitsArrayWithNumber.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        // TODO if we don't want to consider overflowing the size should be varSize + 1
        // c = a + b
        val (cPrim, _) = Variable.BitsArrayWithNumber.create(
            size = varSize,
            bitScheduler = bitScheduler
        )
        val c = cPrim.bitsArray

        val c0 = Equality(
            c[0],
            Xor(
                a.bitsArray[0],
                b.bitsArray[0]
            )
        )
        system.add(c0)

        val pArr = bitScheduler.getAndShift(varSize)
        var pPrev = pArr[0]
        val p0 = Equality(
            pPrev,
            Conjunction(
                a.bitsArray[0],
                b.bitsArray[0]
            )
        )
        system.add(p0)

        for (i in 1 until varSize) {
            val maj = Maj(
                a.bitsArray[i],
                b.bitsArray[i],
                pPrev
            )
            val xor = Xor(
                Xor(
                    a.bitsArray[i],
                    b.bitsArray[i]
                ),
                pPrev
            )
            pPrev = pArr[i]
            val pI = Equality(
                pPrev,
                maj
            )
            val cI = Equality(
                c[i],
                xor
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
    fun parseMultiply(
        a: Variable.BitsArrayWithNumber,
        b: Variable.BitsArrayWithNumber,
        varSize: Int = INT_BITS,
        bitScheduler: BitScheduler
    ): Pair<Variable.BitsArrayWithNumber, BooleanSystem> {
        if (a.constant != null && b.constant != null) {
            val constC = a.constant * b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = Variable.BitsArrayWithNumber.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        // TODO if we don't want to consider overflowing the size should be 2 * varSize
        // c = a*b
        val (cPrim, _) = Variable.BitsArrayWithNumber.create(
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
                    Equality(
                        tempMult[i][j],
                        Conjunction(a.bitsArray[i], b.bitsArray[j])
                    )
                )
            }
        }

        for (i in 0 until varSize - 1) {
            system.add(
                Equality(
                    sumResMult[0][i],
                    Xor(tempMult[0][i + 1], tempMult[i + 1][0])
                )
            )

            system.add(
                Equality(
                    carryMult[0][i],
                    Conjunction(tempMult[0][i + 1], tempMult[i + 1][0])
                )
            )
        }

        for (i in 0 until varSize - 1) {
            for (j in 0 until varSize - 1) {
                system.add(
                    Equality(
                        sumResMult[i + 1][j],
                        Xor(
                            carryMult[i][j],
                            Xor(
                                sumResMult[i][j + 1],
                                tempMult[j + 1][i + 1]
                            )
                        )
                    )
                )

                system.add(
                    Equality(
                        carryMult[i + 1][j],
                        Disjunction(
                            Conjunction(tempMult[j + 1][i + 1], carryMult[i][j]),
                            Disjunction(
                                Conjunction(tempMult[j + 1][i + 1], sumResMult[i][j + 1]),
                                Conjunction(carryMult[i][j], sumResMult[i][j + 1])
                            )
                        )
                    )
                )
            }
        }

        system.add(
            Equality(c[0], tempMult[0][0])
        )

        for (i in 1 until varSize) {
            system.add(
                Equality(c[i], sumResMult[i - 1][0])
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
        a: Variable.BitsArrayWithNumber,
        b: Variable.BitsArrayWithNumber,
        varSize: Int = INT_BITS,
        bitScheduler: BitScheduler
    ): Pair<Variable.BitsArrayWithNumber, BooleanSystem> {
        // a - b

        if (a.constant != null && b.constant != null) {
            val constC = a.constant - b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = Variable.BitsArrayWithNumber.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        /*
         *  Logic is next:
         *  a - b = a + (2^N - b) = a + ([N bits of 1] - [b_N ... b_1] + 1) = a + ~b + 1
         */

        // bInverted = ~b
        val (bInverted, _) = Variable.BitsArrayWithNumber.create(
            size = b.bitsArray.size,
            bitScheduler = bitScheduler
        )

        for (i in 0 until b.bitsArray.size) {
            system.add(
                Equality(
                    bInverted.bitsArray[i],
                    Negated(b.bitsArray[i])
                )
            )
        }

        val (one, oneSystem) = parsePush(1, bitScheduler)
        system.addAll(oneSystem)

        val (bInvertedPlusOne, plusOneSystem) = parseSum(bInverted, one, varSize, bitScheduler)
        system.addAll(plusOneSystem)

        val (c, sumSystem) = parseSum(a, bInvertedPlusOne, varSize, bitScheduler)
        system.addAll(sumSystem)

        return Pair(c, system)
    }

    @JvmStatic
    fun parseXor(
        a: Variable.BitsArrayWithNumber,
        b: Variable.BitsArrayWithNumber,
        varSize: Int = INT_BITS,
        bitScheduler: BitScheduler
    ): Pair<Variable.BitsArrayWithNumber, BooleanSystem> {
        if (a.constant != null && b.constant != null) {
            val constC = a.constant xor b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = Variable.BitsArrayWithNumber.create(
                size = varSize,
                constant = cValue,
                bitScheduler = bitScheduler
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        val (c, _) = Variable.BitsArrayWithNumber.create(
            size = varSize,
            bitScheduler = bitScheduler
        )
        val cArr = c.bitsArray

        for (i in 0 until a.bitsArray.size) {
            system.add(
                Equality(
                    cArr[i],
                    Xor(
                        a.bitsArray[i],
                        b.bitsArray[i]
                    )
                )
            )
        }

        return Pair(c, system)
    }

    @JvmStatic
    fun parsePush(value: Number, bitScheduler: BitScheduler): Pair<Variable.BitsArrayWithNumber, BooleanSystem> {
        // TODO add varSize for longs

        return Variable.BitsArrayWithNumber.create(
            size = INT_BITS,
            constant = value,
            bitScheduler = bitScheduler
        )
    }

    @JvmStatic
    fun parseLessCondition(
        a: Variable.BitsArrayWithNumber,
        b: Variable.BitsArrayWithNumber,
        bitScheduler: BitScheduler
    ): Pair<BitVariable, List<Equality>> {
        // Condition: a < b

        val system = emptyList<Equality>().toMutableList()
        // TODO maybe .constant.toInt() is enough
        if (a.constant != null && b.constant != null) {
            val bit = bitScheduler.getAndShift(1).first()
            system.add(
                Equality(
                    bit,
                    BitValue.getByBoolean(a.constant.toLong() < b.constant.toLong())
                )
            )
            return Pair(bit, system)
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)

        system.add(
            Equality(
                systemBits.first(),
                Conjunction(b.bitsArray.first(), Negated(a.bitsArray.first()))
            )
        )

        // TODO works when a.bitsArray.size == b.bitsArray.size
        for (i in 1 until a.bitsArray.size) {
            system.add(
                Equality(
                    systemBits[i],
                    Disjunction(
                        Conjunction(b.bitsArray[i], Negated(a.bitsArray[i])),
                        Conjunction(
                            Equality(b.bitsArray[i], a.bitsArray[i]),
                            systemBits[i - 1]
                        )
                    )
                )
            )
        }

        return Pair(systemBits.last(), system)
    }

    @JvmStatic
    fun parseLessOrEqualCondition(
        a: Variable.BitsArrayWithNumber,
        b: Variable.BitsArrayWithNumber,
        bitScheduler: BitScheduler
    ): Pair<BitVariable, List<Equality>> {
        // Condition: a <= b

        val system = emptyList<Equality>().toMutableList()
        // TODO maybe .constant.toInt() is enough
        if (a.constant != null && b.constant != null) {
            val bit = bitScheduler.getAndShift(1).first()
            system.add(
                Equality(
                    bit,
                    BitValue.getByBoolean(a.constant.toLong() <= b.constant.toLong())
                )
            )
            return Pair(bit, system)
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)

        system.add(
            Equality(
                systemBits.first(),
                Disjunction(
                    Negated(a.bitsArray.first()),
                    Conjunction(
                        a.bitsArray.first(),
                        b.bitsArray.first()
                    )
                )
            )
        )

        // TODO works when a.bitsArray.size == b.bitsArray.size
        for (i in 1 until a.bitsArray.size) {
            system.add(
                Equality(
                    systemBits[i],
                    Disjunction(
                        Conjunction(b.bitsArray[i], Negated(a.bitsArray[i])),
                        Conjunction(
                            Equality(b.bitsArray[i], a.bitsArray[i]),
                            systemBits[i - 1]
                        )
                    )
                )
            )
        }

        return Pair(systemBits.last(), system)
    }

    @JvmStatic
    fun parseGreaterThanZero(a: Variable.BitsArrayWithNumber, bitScheduler: BitScheduler): Pair<BitVariable, BooleanSystem> {

        // a > 0
        val system = emptyList<Equality>().toMutableList()
        if (a.constant != null) {
            val bit = bitScheduler.getAndShift(1).first()
            system.add(
                Equality(
                    bit,
                    BitValue.getByBoolean(a.constant.toLong() > 0)
                )
            )
            return Pair(bit, system)
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)
        system.add(
            Equality(
                systemBits.first(),
                a.bitsArray.first()
            )
        )

        for (i in 1 until a.bitsArray.size) {
            system.add(
                Equality(
                    systemBits[i],
                    Disjunction(
                        a.bitsArray[i],
                        systemBits[i - 1]
                    )
                )
            )
        }

        return Pair(systemBits.last(), system)
    }
}
