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
package ca.ubc.cs.beta.stationpacking.facade;

import java.util.concurrent.TimeUnit;

import lombok.Data;
import lombok.experimental.Builder;

/**
* Created by newmanne on 16/10/15.
*  Options regarding the auto-augmentation feature, which can be used to expand a SATFCServer cache during downtime
*/
@Data
@Builder
public class AutoAugmentOptions {

    // How long should the SATFCFacade remain idle before beginning augmentation
    private double idleTimeBeforeAugmentation = TimeUnit.MINUTES.convert(5, TimeUnit.SECONDS);
    // Turn auto augmnent on / off
    private boolean augment = false;
    // The station configuration folder with the required files to auto augment from
    private String augmentStationConfigurationFolder = null;
    // How long to spend on each generated problem before giving up
    private double augmentCutoff = TimeUnit.HOURS.convert(3, TimeUnit.SECONDS);
    // How often SATFC should poll to see if the facade has been idle
    private double pollingInterval = TimeUnit.MINUTES.convert(5, TimeUnit.SECONDS);

}
