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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.bundles.yaml;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import lombok.Data;

/**
 * A YAML config file.
 * Can either be internal (bundled inside the jar) or external (from the file system)
 */
@Data
public class ConfigFile {

    final String fileName;
    final boolean internal;

    public String getFileAsString() {
        try {
            if (internal) {
                return Resources.toString(Resources.getResource("bundles" + File.separator + fileName + ".yaml"), Charsets.UTF_8);
            } else {
                return Files.toString(new File(fileName), Charsets.UTF_8);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not load in config file", e);
        }
    }

}
