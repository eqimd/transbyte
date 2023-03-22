package operation_parser

import boolean_logic.BooleanVariable
import boolean_logic.Equality
import constants.BooleanSystem
import constants.Constants.INT_BITS
import constants.GlobalSettings
import extension.and
import extension.minus
import extension.or
import extension.plus
import extension.times
import extension.xor
import parsed_types.data.VariableSat

object OperationParser {
    @JvmStatic
    fun parseSum(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val bitScheduler = GlobalSettings.bitScheduler

        val varSize = a.bitsArray.size

        if (a.constant != null && b.constant != null) {
            val constC = a.constant + b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        // TODO if we don't want to consider overflowing the size should be varSize + 1
        // c = a + b
        val (cPrim, _) = VariableSat.Primitive.create(
            size = varSize,
        )
        val c = cPrim.bitsArray

        for (aVer in a.versions) {
            for (bVer in b.versions) {
                val newConst = aVer.key + bVer.key
                val newCondBit = bitScheduler.getAndShift(1).first()
                system.add(
                    Equality(
                        newCondBit,
                        aVer.value,
                        bVer.value
                    )
                )

                system.addAll(
                    cPrim.addVersion(newConst, newCondBit)
                )
            }
        }

        if (a.constant != null) {
            for (bVer in b.versions) {
                val newConst = bVer.key + a.constant
                system.addAll(
                    cPrim.addVersion(newConst, bVer.value)
                )
            }
        }

        if (b.constant != null) {
            for (aVer in a.versions) {
                val newConst = aVer.key + b.constant
                system.addAll(
                    cPrim.addVersion(newConst, aVer.value)
                )
            }
        }

        val (xorBit, xorSystem) = parseXorBits(a.bitsArray[0], b.bitsArray[0])
        system.addAll(xorSystem)

        val c0 = Equality(
            c[0],
            xorBit
        )
        system.add(c0)

        val pArr = bitScheduler.getAndShift(varSize)
        var pPrev = pArr[0]
        val p0 = Equality(
            pPrev,
            a.bitsArray[0],
            b.bitsArray[0]
        )
        system.add(p0)

        for (i in 1 until varSize) {
            val (majBit, majSys) = parseMajorityBits(a.bitsArray[i], b.bitsArray[i], pPrev)
            system.addAll(majSys)

            val (xorInnerAiBi, xorInnerSys) = parseXorBits(a.bitsArray[i], b.bitsArray[i])
            system.addAll(xorInnerSys)

            val (xorWithPrev, xorPrevSys) = parseXorBits(xorInnerAiBi, pPrev)
            system.addAll(xorPrevSys)

            pPrev = pArr[i]
            val pI = Equality(
                pPrev,
                majBit
            )
            val cI = Equality(
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
        x: BooleanVariable.Bit,
        y: BooleanVariable.Bit,
        z: BooleanVariable.Bit,
    ): Pair<BooleanVariable.Bit, BooleanSystem> {
        /*
         * Majority(x, y, z) = (x and y) or (x and z) or (y and z)
         * == not(not(x and y) and not(x and z) and not (y and z))
         */

        val bitScheduler = GlobalSettings.bitScheduler

        val conjBits = bitScheduler.getAndShift(3)
        val bitXY = conjBits[0]
        val bitXZ = conjBits[1]
        val bitYZ = conjBits[2]

        val system = listOf(
            Equality(bitXY, x, y),
            Equality(bitXZ, x, z),
            Equality(bitYZ, y, z)
        ).toMutableList()

        val (disjFirst, disjFirstSys) = parseDisjunctionBits(bitXY, bitXZ)
        system.addAll(disjFirstSys)

        val (disjSecond, disjSecondSys) = parseDisjunctionBits(disjFirst, bitYZ)
        system.addAll(disjSecondSys)

        return Pair(disjSecond, system)
    }

    @JvmStatic
    fun parseDisjunctionBits(
        x: BooleanVariable.Bit,
        y: BooleanVariable.Bit,
    ): Pair<BooleanVariable.Bit, BooleanSystem> {
        /*
         * X or Y == not(not X and not Y)
         * == not A
         * One new bit: A
         */

        val bitScheduler = GlobalSettings.bitScheduler

        val disjBit = bitScheduler.getAndShift(1).first() // A

        val system = listOf(
            Equality(
                disjBit,
                x.negated(),
                y.negated()
            )
        )

        return Pair(disjBit.negated(), system)
    }

    @JvmStatic
    fun parseEqualityBits(
        x: BooleanVariable.Bit,
        y: BooleanVariable.Bit,
    ): Pair<BooleanVariable.Bit, BooleanSystem> {
        /*
         * Equality(x, y) = (x and y) or (not x and not y)
         */

        val bitScheduler = GlobalSettings.bitScheduler

        val conjBits = bitScheduler.getAndShift(2)
        val lhsBit = conjBits[0]
        val rhsBit = conjBits[1]

        val system = listOf(
            Equality(lhsBit, x, y),
            Equality(rhsBit, x.negated(), y.negated())
        ).toMutableList()

        val (disjBit, disjSys) = parseDisjunctionBits(lhsBit, rhsBit)
        system.addAll(disjSys)

        return Pair(disjBit, system)
    }

    @JvmStatic
    fun parseMultiply(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val bitScheduler = GlobalSettings.bitScheduler

        val varSize = a.bitsArray.size

        if (a.constant != null && b.constant != null) {
            val constC = a.constant * b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        // TODO if we don't want to consider overflowing the size should be 2 * varSize
        // c = a*b
        val (cPrim, _) = VariableSat.Primitive.create(
            size = varSize,
        )
        val c = cPrim.bitsArray

        for (aVer in a.versions) {
            for (bVer in b.versions) {
                val newConst = aVer.key * bVer.key
                val newCondBit = bitScheduler.getAndShift(1).first()
                system.add(
                    Equality(
                        newCondBit,
                        aVer.value,
                        bVer.value
                    )
                )

                system.addAll(
                    cPrim.addVersion(newConst, newCondBit)
                )
            }
        }

        if (a.constant != null) {
            for (bVer in b.versions) {
                val newConst = bVer.key * a.constant
                system.addAll(
                    cPrim.addVersion(newConst, bVer.value)
                )
            }
        }

        if (b.constant != null) {
            for (aVer in a.versions) {
                val newConst = aVer.key * b.constant
                system.addAll(
                    cPrim.addVersion(newConst, aVer.value)
                )
            }
        }

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
                        a.bitsArray[i],
                        b.bitsArray[j]
                    )
                )
            }
        }

        for (i in 0 until varSize - 1) {
            val (xor, xorSys) = parseXorBits(tempMult[0][i + 1], tempMult[i + 1][0])
            system.addAll(xorSys)
            system.add(
                Equality(
                    sumResMult[0][i],
                    xor
                )
            )

            system.add(
                Equality(
                    carryMult[0][i],
                    tempMult[0][i + 1],
                    tempMult[i + 1][0]
                )
            )
        }

        for (i in 0 until varSize - 1) {
            for (j in 0 until varSize - 1 - i - 1) {
                val (xorInnerBit, xorInnerSys) = parseXorBits(
                    sumResMult[i][j + 1],
                    tempMult[j + 1][i + 1]
                )
                system.addAll(xorInnerSys)

                val (xorOuterBit, xorOuterSys) = parseXorBits(
                    carryMult[i][j],
                    xorInnerBit
                )
                system.addAll(xorOuterSys)

                system.add(
                    Equality(sumResMult[i + 1][j], xorOuterBit)
                )

                val conjBits = bitScheduler.getAndShift(3)
                val conj1 = conjBits[0]
                val conj2 = conjBits[1]
                val conj3 = conjBits[2]
                system.addAll(
                    listOf(
                        Equality(conj1, tempMult[j + 1][i + 1], carryMult[i][j]),
                        Equality(conj2, tempMult[j + 1][i + 1], sumResMult[i][j + 1]),
                        Equality(conj3, carryMult[i][j], sumResMult[i][j + 1])
                    )
                )

                val (disjInnerBit, disjInnerSys) = parseDisjunctionBits(conj2, conj3)
                system.addAll(disjInnerSys)

                val (disjOuterBit, disjOuterSys) = parseDisjunctionBits(conj1, disjInnerBit)
                system.addAll(disjOuterSys)
                system.add(
                    Equality(
                        carryMult[i + 1][j],
                        disjOuterBit
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
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        // a - b

        val bitScheduler = GlobalSettings.bitScheduler

        val varSize = a.bitsArray.size

        if (a.constant != null && b.constant != null) {
            val constC = a.constant - b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        /*
         *  Logic is next:
         *  a - b = a + (2^N - b) = a + ([N bits of 1] - [b_N ... b_1] + 1) = a + ~b + 1
         */

        // bInverted = ~b
        val (bInverted, _) = VariableSat.Primitive.create(
            size = b.bitsArray.size,
        )

        for (i in 0 until b.bitsArray.size) {
            system.add(
                Equality(
                    bInverted.bitsArray[i],
                    b.bitsArray[i].negated()
                )
            )
        }

        val (one, oneSystem) = parsePush(1)
        system.addAll(oneSystem)

        val (bInvertedPlusOne, plusOneSystem) = parseSum(bInverted, one)
        system.addAll(plusOneSystem)

        val (c, sumSystem) = parseSum(a, bInvertedPlusOne)
        system.addAll(sumSystem)

        c.clearVersions()

        for (aVer in a.versions) {
            for (bVer in b.versions) {
                val newConst = aVer.key - bVer.key
                val newCondBit = bitScheduler.getAndShift(1).first()
                system.add(
                    Equality(
                        newCondBit,
                        aVer.value,
                        bVer.value
                    )
                )

                system.addAll(
                    c.addVersion(newConst, newCondBit)
                )
            }
        }

        if (a.constant != null) {
            for (bVer in b.versions) {
                val newConst = bVer.key - a.constant
                system.addAll(
                    c.addVersion(newConst, bVer.value)
                )
            }
        }

        if (b.constant != null) {
            for (aVer in a.versions) {
                val newConst = aVer.key - b.constant
                system.addAll(
                    c.addVersion(newConst, aVer.value)
                )
            }
        }

        return Pair(c, system)
    }

    @JvmStatic
    fun parseXorBits(
        x: BooleanVariable.Bit,
        y: BooleanVariable.Bit,
    ): Pair<BooleanVariable.Bit, BooleanSystem> {
        /*
         * X xor Y == (X and not Y) or (not X and Y)
         * == not (not (X and not Y) and not (not X and Y))
         * == not ( not A and not B)
         * == not C
         * Three new bits: A, B, C
         */

        val bitScheduler = GlobalSettings.bitScheduler

        val xorBits = bitScheduler.getAndShift(3)

        val xorLhsBit = xorBits[0] // A
        val xorLhsNegatedBit = xorLhsBit.negated() // not A

        val xorRhsBit = xorBits[1] // B
        val xorRhsNegatedBit = xorRhsBit.negated() // not B

        val xorFullBit = xorBits[2] // C
        val xorFullNegatedBit = xorFullBit.negated() // not C

        val system = listOf(
            Equality(
                xorLhsBit,
                x,
                y.negated()
            ),
            Equality(
                xorRhsBit,
                x.negated(),
                y
            ),
            Equality(
                xorFullBit,
                xorLhsNegatedBit,
                xorRhsNegatedBit
            )
        )

        return Pair(xorFullNegatedBit, system)
    }

    @JvmStatic
    fun parseAnd(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val varSize = a.bitsArray.size

        val bitScheduler = GlobalSettings.bitScheduler

        if (a.constant != null && b.constant != null) {
            val constC = a.constant and b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        val (c, _) = VariableSat.Primitive.create(
            size = varSize,
        )
        val cArr = c.bitsArray

        for (aVer in a.versions) {
            for (bVer in b.versions) {
                val newConst = aVer.key and bVer.key
                val newCondBit = bitScheduler.getAndShift(1).first()
                system.add(
                    Equality(
                        newCondBit,
                        aVer.value,
                        bVer.value
                    )
                )

                system.addAll(
                    c.addVersion(newConst, newCondBit)
                )
            }
        }

        if (a.constant != null) {
            for (bVer in b.versions) {
                val newConst = bVer.key and a.constant
                system.addAll(
                    c.addVersion(newConst, bVer.value)
                )
            }
        }

        if (b.constant != null) {
            for (aVer in a.versions) {
                val newConst = aVer.key and b.constant
                system.addAll(
                    c.addVersion(newConst, aVer.value)
                )
            }
        }

        for (i in 0 until a.bitsArray.size) {
            system.add(
                Equality(
                    cArr[i],
                    a.bitsArray[i],
                    b.bitsArray[i]
                )
            )
        }

        return Pair(c, system)
    }

    @JvmStatic
    fun parseOr(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val varSize = a.bitsArray.size

        val bitScheduler = GlobalSettings.bitScheduler

        if (a.constant != null && b.constant != null) {
            val constC = a.constant or b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        val (c, _) = VariableSat.Primitive.create(
            size = varSize,
        )
        val cArr = c.bitsArray

        for (aVer in a.versions) {
            for (bVer in b.versions) {
                val newConst = aVer.key or bVer.key
                val newCondBit = bitScheduler.getAndShift(1).first()
                system.add(
                    Equality(
                        newCondBit,
                        aVer.value,
                        bVer.value
                    )
                )

                system.addAll(
                    c.addVersion(newConst, newCondBit)
                )
            }
        }

        if (a.constant != null) {
            for (bVer in b.versions) {
                val newConst = bVer.key or a.constant
                system.addAll(
                    c.addVersion(newConst, bVer.value)
                )
            }
        }

        if (b.constant != null) {
            for (aVer in a.versions) {
                val newConst = aVer.key or b.constant
                system.addAll(
                    c.addVersion(newConst, aVer.value)
                )
            }
        }

        for (i in 0 until a.bitsArray.size) {
            val (or, orSys) = parseDisjunctionBits(a.bitsArray[i], b.bitsArray[i])
            system.addAll(orSys)
            system.add(
                Equality(
                    cArr[i],
                    or
                )
            )
        }

        return Pair(c, system)
    }

    @JvmStatic
    fun parseXor(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val varSize = a.bitsArray.size

        val bitScheduler = GlobalSettings.bitScheduler

        if (a.constant != null && b.constant != null) {
            val constC = a.constant xor b.constant

            val cValue = constC.toInt()
            val (c, parseSystem) = VariableSat.Primitive.create(
                size = varSize,
                constant = cValue,
            )

            return Pair(c, parseSystem)
        }

        val system = emptyList<Equality>().toMutableList()

        val (c, _) = VariableSat.Primitive.create(
            size = varSize,
        )
        val cArr = c.bitsArray

        for (aVer in a.versions) {
            for (bVer in b.versions) {
                val newConst = aVer.key and bVer.key
                val newCondBit = bitScheduler.getAndShift(1).first()
                system.add(
                    Equality(
                        newCondBit,
                        aVer.value,
                        bVer.value
                    )
                )

                system.addAll(
                    c.addVersion(newConst, newCondBit)
                )
            }
        }

        if (a.constant != null) {
            for (bVer in b.versions) {
                val newConst = bVer.key and a.constant
                system.addAll(
                    c.addVersion(newConst, bVer.value)
                )
            }
        }

        if (b.constant != null) {
            for (aVer in a.versions) {
                val newConst = aVer.key * b.constant
                system.addAll(
                    c.addVersion(newConst, aVer.value)
                )
            }
        }

        for (i in 0 until a.bitsArray.size) {
            val (xor, xorSys) = parseXorBits(a.bitsArray[i], b.bitsArray[i])
            system.addAll(xorSys)
            system.add(
                Equality(
                    cArr[i],
                    xor
                )
            )
        }

        return Pair(c, system)
    }

    @JvmStatic
    fun parsePush(value: Number): Pair<VariableSat.Primitive, BooleanSystem> {
        val (primitive, system) = VariableSat.Primitive.create(
            size = INT_BITS,
            constant = value,
        )

        return Pair(primitive, system)
    }

    @JvmStatic
    fun parseLessCondition(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<BooleanVariable, BooleanSystem> {
        // TODO primitive versions encoding
        // Condition: a < b

        val bitScheduler = GlobalSettings.bitScheduler

        val system = emptyList<Equality>().toMutableList()
        if (a.constant != null && b.constant != null) {
            return Pair(
                BooleanVariable.Constant.getByBoolean(a.constant.toLong() < b.constant.toLong()),
                emptyList()
            )
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)

        system.add(
            Equality(
                systemBits.first(),
                b.bitsArray.first(),
                a.bitsArray.first().negated()
            )
        )

        // TODO works when a.bitsArray.size == b.bitsArray.size
        for (i in 1 until a.bitsArray.size) {
            val (eqBit, eqSys) = parseEqualityBits(b.bitsArray[i], a.bitsArray[i])
            system.addAll(eqSys)
            val conjBits = bitScheduler.getAndShift(2)
            val conj1 = conjBits[0]
            val conj2 = conjBits[1]
            system.addAll(
                listOf(
                    Equality(conj1, b.bitsArray[i], a.bitsArray[i].negated()),
                    Equality(conj2, eqBit, systemBits[i - 1]),
                )
            )

            val (disjBit, disjSys) = parseDisjunctionBits(conj1, conj2)
            system.addAll(disjSys)
            system.add(
                Equality(
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
    ): Pair<BooleanVariable, BooleanSystem> {
        // TODO primitive versions encoding
        // Condition: a <= b

        val bitScheduler = GlobalSettings.bitScheduler

        val system = emptyList<Equality>().toMutableList()
        if (a.constant != null && b.constant != null) {
            return Pair(
                BooleanVariable.Constant.getByBoolean(a.constant.toLong() <= b.constant.toLong()),
                emptyList()
            )
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)

        val conjAB = bitScheduler.getAndShift(1).first()
        system.add(
            Equality(conjAB, a.bitsArray.first(), b.bitsArray.first())
        )

        val (disjBitFirst, disjSysFirst) = parseDisjunctionBits(a.bitsArray.first().negated(), conjAB)
        system.addAll(disjSysFirst)
        system.add(
            Equality(
                systemBits.first(),
                disjBitFirst
            )
        )

        // TODO works when a.bitsArray.size == b.bitsArray.size
        for (i in 1 until a.bitsArray.size) {
            val (eqBit, eqSys) = parseEqualityBits(b.bitsArray[i], a.bitsArray[i])
            system.addAll(eqSys)
            val conjBits = bitScheduler.getAndShift(2)
            val conj1 = conjBits[0]
            val conj2 = conjBits[1]
            system.addAll(
                listOf(
                    Equality(conj1, b.bitsArray[i], a.bitsArray[i].negated()),
                    Equality(conj2, eqBit, systemBits[i - 1]),
                )
            )

            val (disjBit, disjSys) = parseDisjunctionBits(conj1, conj2)
            system.addAll(disjSys)
            system.add(
                Equality(
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
    ): Pair<BooleanVariable, BooleanSystem> {
        // TODO primitive versions encoding
        val bitScheduler = GlobalSettings.bitScheduler

        // a > 0
        val system = emptyList<Equality>().toMutableList()
        if (a.versions.isNotEmpty()) {
            return if (a.versions.all { it.key.toInt() > 0 }) {
                Pair(BooleanVariable.Constant.TRUE, system)
            } else {
                Pair(BooleanVariable.Constant.FALSE, system)
            }
        }

        if (a.constant != null) {
            return Pair(
                BooleanVariable.Constant.getByBoolean(a.constant.toLong() > 0),
                emptyList()
            )
        }

        val systemBits = bitScheduler.getAndShift(a.bitsArray.size)
        system.add(
            Equality(
                systemBits.first(),
                a.bitsArray.first()
            )
        )

        for (i in 1 until a.bitsArray.size) {
            val (disj, disjSys) = parseDisjunctionBits(a.bitsArray[i], systemBits[i - 1])
            system.addAll(disjSys)
            system.add(
                Equality(
                    systemBits[i],
                    disj
                )
            )
        }

        return Pair(systemBits.last(), system)
    }

    @JvmStatic
    fun parseEqualsCondition(
        a: VariableSat.Primitive,
        b: VariableSat.Primitive,
    ): Pair<BooleanVariable, BooleanSystem> {
        // TODO primitive versions encoding
        // condition: a == b

        val bitScheduler = GlobalSettings.bitScheduler

        if (a.constant != null && b.constant != null) {
            return Pair(
                BooleanVariable.Constant.getByBoolean(
                    a.constant == b.constant
                ),
                emptyList()
            )
        }

        val system = emptyList<Equality>().toMutableList()

        val finalBits = bitScheduler.getAndShift(a.bitsArray.size - 1)

        val (firstEqual, firstEqualSys) = parseEqualityBits(a.bitsArray.first(), b.bitsArray.first())
        system.addAll(firstEqualSys)

        var retBit = firstEqual

        for (i in 1 until a.bitsArray.size) {
            val (eqBit, eqSystem) = parseEqualityBits(a.bitsArray[i], b.bitsArray[i])
            system.addAll(eqSystem)

            system.add(
                Equality(
                    finalBits[i - 1],
                    retBit,
                    eqBit
                )
            )

            retBit = finalBits[i - 1]
        }

        return Pair(retBit, system)
    }

    @JvmStatic
    fun parseEqualToZero(
        a: VariableSat.Primitive
    ): Pair<BooleanVariable, BooleanSystem> {
        // TODO primitive versions encoding
        // condition: a == 0

        val bitScheduler = GlobalSettings.bitScheduler

        if (a.constant != null) {
            return Pair(
                BooleanVariable.Constant.getByBoolean(
                    a.constant == 0
                ),
                emptyList()
            )
        }

        val system = emptyList<Equality>().toMutableList()

        val finalBits = bitScheduler.getAndShift(a.bitsArray.size - 1)

        var retBit = a.bitsArray.first().negated()

        for (i in 1 until a.bitsArray.size) {
            system.add(
                Equality(
                    finalBits[i - 1],
                    retBit,
                    a.bitsArray[i].negated()
                )
            )

            retBit = finalBits[i - 1]
        }

        return Pair(retBit, system)
    }

    @JvmStatic
    fun parseNewPrimitiveWithCondition(
        conditionBit: BooleanVariable.Bit,
        condTruePrimitive: VariableSat.Primitive,
        condFalsePrimitive: VariableSat.Primitive,
    ): Pair<VariableSat.Primitive, BooleanSystem> {
        val bitScheduler = GlobalSettings.bitScheduler

        val newPrimitive = VariableSat.Primitive.create(
            size = condFalsePrimitive.bitsArray.size
        ).first

        val condLocalsSystem = emptyList<Equality>().toMutableList()

        val negatedConditionBit = conditionBit.negated()

        if (condTruePrimitive.constant != null) {
            condLocalsSystem.addAll(
                newPrimitive.addVersion(condTruePrimitive.constant, conditionBit)
            )
        } else {
            for (v in condTruePrimitive.versions) {
                val encBit = bitScheduler.getAndShift(1).first()
                condLocalsSystem.add(
                    Equality(encBit, v.value, conditionBit)
                )
                condLocalsSystem.addAll(
                    newPrimitive.addVersion(v.key, encBit)
                )
            }
        }

        if (condFalsePrimitive.constant != null) {
            condLocalsSystem.addAll(
                newPrimitive.addVersion(condFalsePrimitive.constant, negatedConditionBit)
            )
        } else {
            for (v in condFalsePrimitive.versions) {
                val encBit = bitScheduler.getAndShift(1).first()
                condLocalsSystem.add(
                    Equality(encBit, v.value, negatedConditionBit)
                )
                condLocalsSystem.addAll(
                    newPrimitive.addVersion(v.key, encBit)
                )
            }
        }

        for (i in 0 until condFalsePrimitive.bitsArray.size) {
            /* Condition:
             *  x == (y and cond) or (z and not cond)
             *  == not ( not(y and cond) and not(z and not cond))
             *  == not (not A and not B)
             *  == not C
             *
             *  Three new bits: A, B, C
             */
            val encBits = bitScheduler.getAndShift(3)

            val lhsBit = encBits[0] // A
            val lhsNegatedBit = lhsBit.negated() // not A
            val rhsBit = encBits[1] // B
            val rhsNegatedBit = rhsBit.negated() // not B
            val fullBit = encBits[2] // C
            val fullNegatedBit = fullBit.negated()

            condLocalsSystem.addAll(
                listOf(
                    Equality(
                        lhsBit,
                        condTruePrimitive.bitsArray[i],
                        conditionBit
                    ),
                    Equality(
                        rhsBit,
                        condFalsePrimitive.bitsArray[i],
                        negatedConditionBit
                    ),
                    Equality(
                        fullBit,
                        lhsNegatedBit,
                        rhsNegatedBit
                    ),
                    Equality(
                        newPrimitive.bitsArray[i],
                        fullNegatedBit
                    )
                )
            )
        }

        return Pair(newPrimitive, condLocalsSystem)
    }
}
