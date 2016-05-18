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
package ca.ubc.cs.beta.stationpacking.execution.metricwriters;

import java.io.File;
import java.io.IOException;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import ca.ubc.cs.beta.stationpacking.metrics.SATFCMetrics;
import ca.ubc.cs.beta.stationpacking.utils.JSONUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by newmanne on 29/05/15.
 * Write metrics to a file on disk, appending each problem's metrics as a line of json
 */
@Slf4j
public class FileMetricsWriter implements IMetricWriter {

    private final File metricsFile;

    public FileMetricsWriter(String metricsFileName) {
        this.metricsFile = new File(metricsFileName);
        if (this.metricsFile.exists()) {
            this.metricsFile.delete();
        }
    }

    @Override
    public void writeMetrics() {
        SATFCMetrics.doWithMetrics(info -> {
            try {
                Files.append(JSONUtils.toString(info) + System.lineSeparator(), metricsFile, Charsets.UTF_8);
            } catch (IOException e) {
                log.error("Couldn't save metrics to file " + metricsFile.getAbsolutePath(), e);
            }
        });
    }

    @Override
    public void onFinished() {
        SATFCMetrics.report();
    }

}
