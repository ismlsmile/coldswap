package org.coldswap.agent;

import org.coldswap.instrumentation.ClassInstrumenter;
import org.coldswap.tracker.ClassWatcher;
import org.coldswap.transformer.*;
import org.coldswap.util.ClassUtil;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;
import java.util.logging.Level;
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
 * 2:01 PM       3/15/13
 */

public class ColdSwapAgent {
    private static final Logger logger = Logger.getLogger(ColdSwapAgent.class.getName());
    private static final ClassInstrumenter instrumenter = ClassInstrumenter.getInstance();

    static {
        logger.setLevel(ClassUtil.logLevel);
    }

    public static void premain(String args, Instrumentation inst) {
        AgentArgsParser argsParser = new AgentArgsParser(args);
        argsParser.buildArgs();
        ClassUtil.logLevel = (Level) argsParser.getArgument("logLevel");
        int maxMethods = (Integer) argsParser.getArgument("maxNumberOfMethods");
        inst.addTransformer(new ClInitTransformer());
        inst.addTransformer(new ObjectMethodTransformer(maxMethods));
        inst.addTransformer(new IntMethodTransformer(maxMethods));
        inst.addTransformer(new FloatMethodTransformer(maxMethods));
        inst.addTransformer(new StringMethodTransformer(maxMethods));
        inst.addTransformer(new LongMethodTransformer(maxMethods));
        instrumenter.setInstrumenter(inst);
        // set java library path for jnotify
        StringBuilder sb = new StringBuilder(System.getProperty("user.home"));
        String separator = System.getProperty("file.separator");
        sb.append(separator).append(".coldswap").append(separator).append("native").append(separator);
        String jlp = System.getProperty("java.library.path");
        System.setProperty("java.library.path", jlp + ":" + sb.toString());
        // here is a fine trick. Usually java.library.path must be set before
        // the application is started. But here's a dirty trick that forces
        // the class loader to automatically reload the library paths by setting a
        // static field to null.
        // Credits goes to folks at https://jdic.dev.java.net/
        Field fieldSysPath;
        try {
            fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
            fieldSysPath.setAccessible(true);
            fieldSysPath.set(null, null);
        } catch (NoSuchFieldException e) {
            logger.severe("Please restart coldswap with this argument: -Djava.library.path=\"" + sb.toString() + "\"\n");
            System.exit(0);
        } catch (IllegalAccessException e) {
            logger.severe("Please restart coldswap with this argument: -Djava.library.path=\"" + sb.toString() + "\"\n");
            System.exit(0);
        }

        String[] dirs;
        ClassWatcher[] monitors;
        try {
            dirs = (String[]) argsParser.getArgument("cp");

            monitors = new ClassWatcher[dirs.length];
        } catch (NullPointerException e) {
            logger.severe("There is no folder to watch for reloading.\n" + e.toString());
            return;
        } catch (ClassCastException e) {
            logger.severe("Invalid values provided to cp argument!\n" + e.toString());
            return;
        }

        String recursive = "false";
        try {
            recursive = (String) argsParser.getArgument("recursive");
        } catch (NullPointerException e) {
            logger.severe("There is no recursive argument provided to ColdSwap.\n" + e.toString());
        } catch (ClassCastException e) {
            logger.severe("Invalid value provided to recursive argument!\n" + e.toString());
        }

        for (int i = 0; i < monitors.length; i++) {
            monitors[i] = new ClassWatcher(dirs[i], "true".equals(recursive), maxMethods);
            Thread t = new Thread(monitors[i]);
            t.setDaemon(true);
            t.start();

        }
    }
}
