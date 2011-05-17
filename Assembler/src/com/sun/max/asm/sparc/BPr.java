/*
 * Copyright (c) 2007, 2008, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.max.asm.sparc;

import com.sun.max.asm.*;
import com.sun.max.util.*;

/**
 * The argument to a Branch on Integer Register with Prediction instruction specifying
 * the conditional test to be performed.
 *
 * @author Bernd Mathiske
 * @author Doug Simon
 */
public final class BPr extends NameSuffixSymbolicArgument {

    private BPr(int value) {
        super(value);
    }

    public static final BPr Z = new BPr(1);
    public static final BPr LEZ = new BPr(2);
    public static final BPr LZ = new BPr(3);
    public static final BPr NZ = new BPr(5);
    public static final BPr GZ = new BPr(6);
    public static final BPr GEZ = new BPr(7);

    public static final Symbolizer<BPr> SYMBOLIZER = Symbolizer.Static.initialize(BPr.class);

}
