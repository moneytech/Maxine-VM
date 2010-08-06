/*
 * Copyright (c) 2007 Sun Microsystems, Inc.  All rights reserved.
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
package jtt.bytecode;

/*
 * @Harness: java
 * @Runs: 0 = false; 1 = true; 2 = true; 3 = false; 4 = true; 5 = true; 6 = false; 7 = false; 8 = false
 */
public class BC_fcmp10 {

    public static boolean test(int x) {
        float a = 0;
        float b = 0;
        switch (x) {
            case 0:
                a = Float.POSITIVE_INFINITY;
                b = 1;
                break;
            case 1:
                a = 1;
                b = Float.POSITIVE_INFINITY;
                break;
            case 2:
                a = Float.NEGATIVE_INFINITY;
                b = 1;
                break;
            case 3:
                a = 1;
                b = Float.NEGATIVE_INFINITY;
                break;
            case 4:
                a = Float.NEGATIVE_INFINITY;
                b = Float.NEGATIVE_INFINITY;
                break;
            case 5:
                a = Float.NEGATIVE_INFINITY;
                b = Float.POSITIVE_INFINITY;
                break;
            case 6:
                a = Float.NaN;
                b = Float.POSITIVE_INFINITY;
                break;
            case 7:
                a = 1;
                b = Float.NaN;
                break;
            case 8:
                a = 1;
                b = -0.0f / 0.0f;
                break;
        }
        return a <= b;
    }
}
