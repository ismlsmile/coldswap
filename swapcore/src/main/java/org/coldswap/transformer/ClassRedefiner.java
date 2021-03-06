package org.coldswap.transformer;

import org.coldswap.asm.MemberReplacer;
import org.coldswap.asm.field.PrivateStaticFieldReplacer;
import org.coldswap.asm.field.ProtectedStaticFieldReplacer;
import org.coldswap.asm.field.PublicStaticFieldReplacer;
import org.coldswap.asm.method.*;
import org.coldswap.instrumentation.ClassInstrumenter;
import org.coldswap.util.ByteCodeClassLoader;
import org.coldswap.util.ClassUtil;

import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.logging.Logger;

/**
 * (C) Copyright 2013 Faur Ioan-Aurel.
 * <p/>
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * Contributors:
 * faur
 * <p/>
 * Created at:
 * 9:05 PM       3/20/13
 */

/**
 * Redefine methods who's body has changed during runtime.
 */
public class ClassRedefiner {
    private ReferenceReplacerManager replacerManager = ReferenceReplacerManager.getInstance();
    private final String clsName;
    private final String path;
    private final int maxMethods;
    private static final Logger logger = Logger.getLogger(ClassRedefiner.class.getName());

    static {
        logger.setLevel(ClassUtil.logLevel);
    }

    /**
     * Constructs a Class method redefiner.
     *
     * @param className Name.class of a modified file.
     * @param classPath Class residing directory.
     */
    public ClassRedefiner(String className, String classPath, int maxNumberOfMethods) {
        this.clsName = className.replace(".class", "").replace(System.getProperty("file.separator"), ".");
        this.path = classPath;
        this.maxMethods = maxNumberOfMethods;
    }

    /**
     * Reloads any modified method body.
     *
     * @param clazz the already existing body of a class.
     * @throws UnmodifiableClassException
     * @throws ClassNotFoundException
     */
    public void redefineClass(Class<?> clazz) throws UnmodifiableClassException, ClassNotFoundException {
        byte[] classBytes = ByteCodeClassLoader.loadClassBytes(this.path);
        // first make run the reference replacer
        classBytes = replacerManager.runReferenceReplacer(classBytes);
        // find new public static fields in the code and replace them
        MemberReplacer psRep = new PublicStaticFieldReplacer(clazz, classBytes);
        classBytes = psRep.replace();
        // find new private static fields in the code and replace them
        // since we search for private fields we don't need to run a reference replacer
        // on other classes.
        MemberReplacer psvRep = new PrivateStaticFieldReplacer(clazz, classBytes);
        classBytes = psvRep.replace();
        // find protected fields and replace them
        MemberReplacer prRep = new ProtectedStaticFieldReplacer(clazz, classBytes);
        classBytes = prRep.replace();
        MemberReplacer nvmoRep = new PublicObjectMethodReplacer(clazz, classBytes, maxMethods);
        classBytes = nvmoRep.replace();
        MemberReplacer nvmiRep = new PublicIntMethodReplacer(clazz, classBytes, maxMethods);
        classBytes = nvmiRep.replace();
        MemberReplacer nvmfRep = new PublicFloatMethodReplacer(clazz, classBytes, maxMethods);
        classBytes = nvmfRep.replace();
        MemberReplacer nvmstrRep = new PublicStringMethodReplacer(clazz, classBytes, maxMethods);
        classBytes = nvmstrRep.replace();
        MemberReplacer nvmlongRep = new PublicLongMethodReplacer(clazz, classBytes, maxMethods);
        classBytes = nvmlongRep.replace();
        // clean garbage methods
        MemberReplacer garbage = new MethodCleaner(clazz, classBytes);
        classBytes = garbage.replace();
        ClassDefinition cls = new ClassDefinition(clazz, classBytes);
        Instrumentation inst = ClassInstrumenter.getInstance().getInstrumenter();
        inst.redefineClasses(cls);
        logger.info("Class " + this.clsName + " was  redefined!");
    }

}
