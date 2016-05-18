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
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import ca.ubc.cs.beta.stationpacking.facade.datamanager.data.DataManager;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by emily404 on 9/23/15.
 */
@Slf4j
public class PythonInterpreterContainer {

    private final ReadWriteLock fLock = new ReentrantReadWriteLock();
    private PythonInterpreter python;

    public PythonInterpreterContainer(String interferenceFolder, boolean compact) {

        python = new PythonInterpreter();
        python.execfile(getClass().getClassLoader().getResourceAsStream("verifier.py"));
        String interference = interferenceFolder + File.separator + DataManager.INTERFERENCES_FILE;
        String domain = interferenceFolder + "/" + DataManager.DOMAIN_FILE;

        String interferenceResult;
        if (compact) {
            final String compactEvalString = "load_compact_interference(\"" + interference + "\")";
            log.debug("Evaluation string feed to Python interpreter: " + compactEvalString);

            final PyObject compactReturnObject = python.eval(compactEvalString);
            interferenceResult = compactReturnObject.toString();
        } else {
            final String nonCompactEvalString = "load_interference(\"" + interference + "\")";
            log.debug("Evaluation string feed to Python interpreter: " + nonCompactEvalString);

            final PyObject nonCompactReturnObject = python.eval(nonCompactEvalString);
            interferenceResult = nonCompactReturnObject.toString();
        }
        final String domainEvalString = "load_domain_csv(\"" + domain + "\")";
        final PyObject loadDomainReturnObject = python.eval(domainEvalString);
        final String domainResult = loadDomainReturnObject.toString();

        if (interferenceResult.equals("0") && domainResult.equals("0")){
            log.debug("Interference loaded");
        } else {
            throw new IllegalStateException("Interference not loaded properly");
        }

    }

    public PyObject eval(String evalString) {
        try{
            fLock.readLock().lock();
            return python.eval(evalString);
        } finally {
            fLock.readLock().unlock();
        }
    }

}
