package org.coldswap.util;

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
 * 3:56 PM       5/5/13
 */

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple static methods with the main purpose to extract informations
 * from {@link Class} files.
 */
public class ClassUtil {
    private static final Logger logger = Logger.getLogger(ClassUtil.class.getName());

    static {
        logger.setLevel(Level.ALL);
    }

    /**
     * Finds where is stored the bytecode of a class.
     *
     * @param aClass {@link Class} type containing intel about a class loaded into JVM.
     * @return returns the parent directory of class
     */
    @SuppressWarnings("ConstantConditions")
    public static String getClassPath(Class aClass) {
        String path;
        path = aClass.getClassLoader().getResource(
                aClass.getName().replace('.', '/') + ".class").toString();
        String parent = null;
        try {
            parent = new File(new URI(path)).getParent();
        } catch (URISyntaxException e) {
            logger.warning(e.toString());
        }
        return parent;
    }
}