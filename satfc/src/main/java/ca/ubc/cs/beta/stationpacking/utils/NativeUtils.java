/**
 * Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
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

import ca.ubc.cs.beta.stationpacking.solvers.sat.solvers.jnalibraries.Clasp3Library;

import com.google.common.collect.ImmutableMap;

/**
 * Created by newmanne on 20/05/15.
 */
public class NativeUtils {

    private final static int RTLD_LOCAL = 0x00000;
    private final static int RTLD_LAZY = 0x00001;

    public static final Map NATIVE_OPTIONS = ImmutableMap.of(Clasp3Library.OPTION_OPEN_FLAGS, RTLD_LAZY | RTLD_LOCAL);


}
