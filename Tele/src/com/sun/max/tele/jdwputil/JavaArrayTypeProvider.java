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
package com.sun.max.tele.jdwputil;

import java.lang.reflect.*;

import com.sun.max.jdwp.vm.core.*;
import com.sun.max.jdwp.vm.proxy.*;


public class JavaArrayTypeProvider extends JavaClassProvider implements ArrayTypeProvider {

    private ReferenceTypeProvider _elementType;
    private Class _clazz;
    private VMAccess _vm;


    JavaArrayTypeProvider(Class c, VMAccess vm, ClassLoaderProvider classLoader) {
        super(c, vm, classLoader);
        _elementType = vm.getReferenceType(c.getComponentType());
        _clazz = c;
        _vm = vm;
    }

    public ReferenceTypeProvider elementType() {
        return _elementType;
    }

    public ArrayProvider newInstance(int length) {
        final Object array = Array.newInstance(_clazz.getComponentType(), length);
        final Provider result = _vm.createJavaObjectValue(array, _clazz).asProvider();
        assert result instanceof ArrayProvider;
        return (ArrayProvider) result;
    }


}
