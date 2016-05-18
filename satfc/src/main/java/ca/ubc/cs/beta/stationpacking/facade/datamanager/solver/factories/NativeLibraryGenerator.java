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
package ca.ubc.cs.beta.stationpacking.facade.datamanager.solver.factories;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.common.io.Files;
import com.sun.jna.Library;
import com.sun.jna.Native;

import ca.ubc.cs.beta.stationpacking.utils.NativeUtils;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * This class returns a fresh, independent clasp library to each thread
 * We need to use this class because clasp is not re-entrant
 * Because of the way JNA works, we need a new physical copy of the library each time we launch
 * Also note the LD_OPEN options specified to Native.loadLibrary to make sure we use a different in-memory copy each time
 */
public abstract class NativeLibraryGenerator<T extends Library> {
    @Getter(AccessLevel.PUBLIC)
    private final String libraryPath;
    private final Class<T> klazz;
    private int numLibs;

    private List<File> libFiles;

    public NativeLibraryGenerator(String libraryPath, Class<T> klazz) {
        this.libraryPath = libraryPath;
        this.klazz = klazz;
        numLibs = 0;
        libFiles = new ArrayList<>();
    }

    @SuppressWarnings("unchecked")
	public T createLibrary() {
        File origFile = new File(libraryPath);
        try {
            File copy = File.createTempFile(Files.getNameWithoutExtension(libraryPath) + "_" + ++numLibs, "." + Files.getFileExtension(libraryPath));
            Files.copy(origFile, copy);
            copy.deleteOnExit();
            libFiles.add(copy);
            return (T) Native.loadLibrary(copy.getPath(), klazz, NativeUtils.NATIVE_OPTIONS);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't create a copy of native library from path " + libraryPath);
        }
    }

    public void notifyShutdown() {
        libFiles.forEach(File::delete);
    }

}
