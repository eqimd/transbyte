package instruction_parser

import bit_scheduler.BitScheduler
import boolean_logic.BooleanFormula
import boolean_logic.additional.Equality
import boolean_logic.additional.Maj
import boolean_logic.additional.Xor
import boolean_logic.base.BitValue
import boolean_logic.base.Conjunction
import boolean_logic.base.Disjunction
import boolean_logic.base.Negated
import constants.Constants.INT_BITS
import extension.plus
import extension.times
import parsed_types.data.Variable

class InstructionParser private constructor() {
    companion object {
        @JvmStatic
        fun parseADD(
            a: Variable.BitsArrayWithNumber,
            b: Variable.BitsArrayWithNumber,
            bitScheduler: BitScheduler,
            varSize: Int = INT_BITS,
        ): Pair<Variable.BitsArrayWithNumber, List<BooleanFormula>> {
            if (a.constant != null && b.constant != null) {
                val constC = a.constant + b.constant
                val cArray = bitScheduler.getAndShift(varSize)
                val c = Variable.BitsArrayWithNumber(cArray, constC)

                val parseSystem: List<BooleanFormula>
                if (varSize == INT_BITS) {
                    val cValue = constC.toInt()
                    parseSystem = List(varSize) { index ->
                        Equality(
                            cArray[index],
                            BitValue.getByBoolean(cValue.shr(index).and(1) != 0)
                        )
                    }
                } else {
                    val cValue = constC.toLong()
                    parseSystem = List(varSize) { index ->
                        Equality(
                            cArray[index],
                            BitValue.getByBoolean((cValue.shr(index).and(1) != 0L))
                        )
                    }
                }

                return Pair(c, parseSystem)
            }

            val system = emptyList<BooleanFormula>().toMutableList()

            // TODO if we don't want to consider overflowing the size should be varSize + 1
            // c = a + b
            val c = bitScheduler.getAndShift(varSize)

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

            return Pair(
                Variable.BitsArrayWithNumber(c),
                system
            )
        }

        @JvmStatic
        fun parseMUL(
            a: Variable.BitsArrayWithNumber,
            b: Variable.BitsArrayWithNumber,
            bitScheduler: BitScheduler,
            varSize: Int = INT_BITS,
        ): Pair<Variable.BitsArrayWithNumber, List<BooleanFormula>> {
            if (a.constant != null && b.constant != null) {
                val constC = a.constant * b.constant
                val cArray = bitScheduler.getAndShift(varSize)
                val c = Variable.BitsArrayWithNumber(cArray, constC)

                val parseSystem: List<BooleanFormula>
                if (varSize == INT_BITS) {
                    val cValue = constC.toInt()
                    parseSystem = List(varSize) { index ->
                        Equality(
                            cArray[index],
                            BitValue.getByBoolean((cValue.shr(index).and(1) != 0))
                        )
                    }
                } else {
                    val cValue = constC.toLong()
                    parseSystem = List(varSize) { index ->
                        Equality(
                            cArray[index],
                            BitValue.getByBoolean((cValue.shr(index).and(1) != 0L))
                        )
                    }
                }

                return Pair(c, parseSystem)
            }

            val system = emptyList<BooleanFormula>().toMutableList()

            // TODO if we don't want to consider overflowing the size should be 2 * varSize
            // c = a*b
            val c = bitScheduler.getAndShift(varSize)

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

            return Pair(
                Variable.BitsArrayWithNumber(c),
                system
            )
        }

        @JvmStatic
        fun parsePUSH(value: Number, bitScheduler: BitScheduler): Pair<Variable.BitsArrayWithNumber, List<BooleanFormula>> {
            val bitsArray = bitScheduler.getAndShift(INT_BITS)

            val intValue = value.toInt()

            val parseSystem = List(INT_BITS) { index ->
                Equality(
                    bitsArray[index],
                    BitValue.getByBoolean((intValue.shl(index) != 0))
                )
            }

            val retArray = Variable.BitsArrayWithNumber(bitsArray, value)

            return Pair(retArray, parseSystem)
        }

        @JvmStatic
        fun parseLessCondition(a: Variable.BitsArrayWithNumber, b: Variable.BitsArrayWithNumber): BooleanFormula {
            // Condition: a < b

            // TODO maybe .constant.toInt() is enough
            if (a.constant != null && b.constant != null) {
                return BitValue.getByBoolean(a.constant.toLong() < b.constant.toLong())
            }

            var cond: BooleanFormula = Conjunction(b.bitsArray.first(), Negated(a.bitsArray.first()))

            // TODO works when a.bitsArray.size == b.bitsArray.size
            for (i in 1 until a.bitsArray.size) {
                cond = Disjunction(
                    Conjunction(b.bitsArray[i], Negated(a.bitsArray[i])),
                    Conjunction(
                        Equality(b.bitsArray[i], a.bitsArray[i]),
                        cond
                    )
                )
            }

            return cond
        }
    }
}
