/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package com.sun.c1x.ir;

import com.sun.cri.bytecode.Bytecodes.*;
import com.sun.cri.ci.*;
import com.sun.cri.ri.*;

/**
 * Condition codes used in conditionals.
 */
public enum Condition {
    /**
     * Equal.
     */
    EQ("=="),

    /**
     * Not equal.
     */
    NE("!="),

    /**
     * Signed less than.
     */
    LT("<"),

    /**
     * Signed less than or equal.
     */
    LE("<="),

    /**
     * Signed greater than.
     */
    GT(">"),

    /**
     * Signed greater than or equal.
     */
    GE(">="),

    /**
     * Unsigned greater than or equal ("above than or equal").
     */
    AE("|>=|"),

    /**
     * Unsigned less than or equal ("below than or equal").
     */
    BE("|<=|"),

    /**
     * Unsigned greater than ("above than").
     */
    AT("|>|"),

    /**
     * Unsigned less than ("below than").
     */
    BT("|<|"),

    TRUE("TRUE");

    public final String operator;

    private Condition(String operator) {
        this.operator = operator;
    }

    /**
     * Negate this conditional.
     * @return the condition that represents the negation
     */
    public final Condition negate() {
        switch (this) {
            case EQ: return NE;
            case NE: return EQ;
            case LT: return GE;
            case LE: return GT;
            case GT: return LE;
            case GE: return LT;
            case BT: return AE;
            case BE: return AT;
            case AT: return BE;
            case AE: return BT;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Mirror this conditional (i.e. commute "a op b" to "b op' a")
     * @return the condition representing the equivalent commuted operation
     */
    public final Condition mirror() {
        switch (this) {
            case EQ: return EQ;
            case NE: return NE;
            case LT: return GT;
            case LE: return GE;
            case GT: return LT;
            case GE: return LE;
            case BT: return AT;
            case BE: return AE;
            case AT: return BT;
            case AE: return BE;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Checks if this conditional operation is commutative.
     * @return {@code true} if this operation is commutative
     */
    public final boolean isCommutative() {
        return this == EQ || this == NE;
    }

    /**
     * Attempts to fold a comparison between two constants and return the result.
     * @param lt the constant on the left side of the comparison
     * @param rt the constant on the right side of the comparison
     * @param runtime the RiRuntime (might be needed to compare runtime-specific types)
     * @return {@link Boolean#TRUE} if the comparison is known to be true,
     * {@link Boolean#FALSE} if the comparison is known to be false, <code>null</code> otherwise.
     */
    public Boolean foldCondition(CiConstant lt, CiConstant rt, RiRuntime runtime) {
        switch (lt.kind) {
            case Int: {
                int x = lt.asInt();
                int y = rt.asInt();
                switch (this) {
                    case EQ: return x == y;
                    case NE: return x != y;
                    case LT: return x < y;
                    case LE: return x <= y;
                    case GT: return x > y;
                    case GE: return x >= y;
                    case AE: return UnsignedComparisons.aboveOrEqual(x, y);
                    case BE: return UnsignedComparisons.belowOrEqual(x, y);
                    case AT: return UnsignedComparisons.aboveThan(x, y);
                    case BT: return UnsignedComparisons.belowThan(x, y);
                }
                break;
            }
            case Long: {
                long x = lt.asLong();
                long y = rt.asLong();
                switch (this) {
                    case EQ: return x == y;
                    case NE: return x != y;
                    case LT: return x < y;
                    case LE: return x <= y;
                    case GT: return x > y;
                    case GE: return x >= y;
                }
                break;
            }
            case Object: {
                Object x = lt.asObject();
                Object y = rt.asObject();
                switch (this) {
                    case EQ: return runtime.compareConstantObjects(x, y);
                    case NE: return !runtime.compareConstantObjects(x, y);
                }
                break;
            }
            // XXX: folding of floating comparisons should be possible
        }
        return null;
    }
}
