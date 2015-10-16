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

import com.google.common.base.Joiner;

/**
 * Created by newmanne on 11/05/15.
 */
public class RedisUtils {

    public static final String TIMEOUTS_QUEUE = "TIMEOUTS";
    public static final String PROCESSING_QUEUE = "PROCESSING";
    public static final String JOB_QUEUE = "_JOB";
    public static final String JSON_HASH = "_JSON";
    public static final String CNF_INDEX_QUEUE = "CNFIndex";

    public static String makeKey(String... args) {
        return Joiner.on(':').join(args);
    }

}
