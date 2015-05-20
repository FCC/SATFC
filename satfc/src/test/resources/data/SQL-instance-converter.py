#
# Copyright 2015, Auctionomics, Alexandre Fréchette, Neil Newman, Kevin Leyton-Brown.
#
# This file is part of SATFC.
#
# SATFC is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# SATFC is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with SATFC.  If not, see <http://www.gnu.org/licenses/>.
#
# For questions, contact us at:
# afrechet@cs.ubc.ca
#

import sys


def printInstance(S):

    S = S.replace('\n','')

    parts = S.split('_')

    data_foldername = parts[0]

    domains = {}
    previous_assignment = {}
    for part in parts[1:]:
        stationchannels = part.split(';')

        station = int(stationchannels[0])

        previous_channel = int(stationchannels[1])
        if previous_channel > 0:
            if station in previous_assignment:
                raise Exception('Station %d is already in previous assignment.' % station)
            else:
                previous_assignment[station] = previous_channel

        domain = []
        for channelstring in stationchannels[2].split(','):
            channel = int(channelstring)
            if channel in domain:
                raise Exception("Channel %d is already in station %d's domain." % (channel,station))
            else:
                domain.append(channel)

        if station in domains:
            raise Exception('Station %d is already present in domains.' % station)
        else:
            domains[station] = domain

    print data_foldername
    print ';'.join(['%d:%s' % (station,','.join([str(c) for c in domains[station]])) for station in domains])

        

if len(sys.argv) > 1:
    for arg in sys.argv[1:]:
        printInstance(arg)
else:
    for line in sys.stdin:
        printInstance(line)

