/**
 * Copyright 2016, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
 *
 * This file is part of SATFC.
 *
 * SATFC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * SATFC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For questions, contact us at:
 * afrechet@cs.ubc.ca
 */
package ca.ubc.cs.beta.stationpacking.utils;

import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.sun.jna.Library;
import com.sun.jna.Platform;

/**
 * Created by newmanne on 20/05/15.
 */
public class NativeUtils {

    private final static int RTLD_LOCAL;
    private final static int RTLD_LAZY;
    public static final Map NATIVE_OPTIONS;

    static {
        if (Platform.isMac()) {
            RTLD_LOCAL = 0x00000004;
            RTLD_LAZY = 0x00000001;
        } else {
            if (!Platform.isLinux()) {
                System.err.println("OS was not detected as mac or linux. Assuming values for <dlfcn.h> RTLD_LOCAL and RTLD_LAZY are same as linux. Unexpected errors can occur if this is not the case");
            }
            RTLD_LOCAL = 0x00000000;
            RTLD_LAZY = 0x00000001;
        }
        NATIVE_OPTIONS = ImmutableMap.of(Library.OPTION_OPEN_FLAGS, RTLD_LAZY | RTLD_LOCAL);
    }

}
