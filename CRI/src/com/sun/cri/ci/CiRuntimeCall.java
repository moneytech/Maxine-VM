/*
 * Copyright (c) 2009 Sun Microsystems, Inc.  All rights reserved.
 *
 * Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product
 * that is described in this document. In particular, and without limitation, these intellectual property
 * rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or
 * more additional patents or pending patent applications in the U.S. and in other countries.
 *
 * U.S. Government Rights - Commercial software. Government users are subject to the Sun
 * Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its
 * supplements.
 *
 * Use is subject to license terms. Sun, Sun Microsystems, the Sun logo, Java and Solaris are trademarks or
 * registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries. All SPARC trademarks
 * are used under license and are trademarks or registered trademarks of SPARC International, Inc. in the
 * U.S. and other countries.
 *
 * UNIX is a registered trademark in the U.S. and other countries, exclusively licensed through X/Open
 * Company, Ltd.
 */
package com.sun.cri.ci;

import static com.sun.cri.ci.CiKind.*;

/**
 * Enumerates the calls that must be provided by the runtime system. The compiler
 * may generate code that calls the runtime services for unresolved and slow cases of some
 * bytecodes.
 *
 * @author Marcelo Cintra
 * @author Thomas Wuerthinger
 * @author Ben L. Titzer
 */
public enum CiRuntimeCall {
    UnwindException(Void, Object),
    ThrowArithmeticException(Void),
    RegisterFinalizer(Void, Object),
    HandleException(Void, Object),
    OSRMigrationEnd(Void),
    JavaTimeMillis(Long),
    JavaTimeNanos(Long),
    Debug(Void),
    ArithmethicLrem(Long, Long, Long),
    ArithmeticLdiv(Long, Long, Long),
    ArithmeticFrem(Float, Float, Float),
    ArithmeticDrem(Double, Double, Double),
    ArithmeticCos(Double, Double),
    ArithmeticTan(Double, Double),
    ArithmeticLog(Double, Double),
    ArithmeticLog10(Double, Double),
    ArithmeticSin(Double, Double);

    public final CiKind resultKind;
    public final CiKind[] arguments;

    private CiRuntimeCall(CiKind resultKind, CiKind... args) {
        this.resultKind = resultKind;
        this.arguments = args;
    }
}