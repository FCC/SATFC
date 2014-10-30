# Feasibility Checker

## Introduction

SATFC (_SAT-based Feasibility Checker_) solves radio-spectrum repacking feasibility problems arising in the reverse auction of the FCC's upcoming broadcast incentive auction. It combines a formulation of feasibility checking based on propositional satisfiability with a heuristic pre-solver and a SAT solver tuned for the types of instances observed in auction simulations.
authors & collaborators SATFC is the product of the ideas and hard work of Auctionomics, notably Alexandre Fréchette, Guillaume Saulnier-Comte, Nick Arnosti, and Kevin Leyton-Brown.

Questions, bug reports and feature suggestions should be directed to Alexandre Fréchette – afrechet@cs.ubc.ca.

### FCC Releases

* FCC's [LEARN Repacking page](http://wireless.fcc.gov/incentiveauctions/learn-program/repacking.html)
* [Incentive Auction Report & Order](https://apps.fcc.gov/edocs_public/attachmatch/FCC-14-50A1.pdf)
* [Feasibility Checking PN](http://hraunfoss.fcc.gov/edocs_public/attachmatch/DA-14-3A1.pdf)

### License

SATFC is released under the GNU General Public License (GPL) - http://www.gnu.org/copyleft/gpl.html.

### System Requirements

SATFC is primarily intended to run on Unix-like platforms. It requires Java 7 to run (although we have observed greater stability with Java 8). One also needs our modified version of the SAT solver clasp v2.2.3 compiled for JNA library usage, which in turn necessitates gcc v4.8.1 or higher as well as the standard Unix C libraries (see below to learn how to compile clasp).

### Version

This manual is for SATFC v1.1.0a.

## Description

### Problem

At the core of the reverse auction part of the radio-spectrum incentive auction lies the problem of (re)assigning television stations to a smaller range of broadcast channels than what they currently occupy subject to various interference constraints (often referred to as the Station Packing Problem or the Feasibility Checking Problem). These constraints are pairwise between stations, hence the whole problem can be cast to an (extended) Graph Coloring Problem, where stations are nodes of the graph, edges correspond to interference constraints and colors to available broadcast channels. Unfortunately, the latter problem is known to be NP-complete, _i.e._ computationally challenging. Furthermore, in the latest reverse auction designs, feasibility checking must be executed very frequently, sometimes upwards of a thousand times per auction round. This problem is thus the fundamental computational bottleneck of the auction. Fortunately, the distribution of feasibility checking problems encountered in a typical auction is very specific, and that is what SATFC leverages.

### SATFC

To take advantage of the specific distribution of encountered feasibility checking problems, SATFC first translates feasibility checking into its propositional satisfiability (SAT) version. This allows us to leverage the body of research on high performance SAT solvers developed over the last years. Through extensive empirical evaluations, we have identified the SAT solver clasp as the best solver when tuned on our specific instance set using SMAC, a sequential, model-based algorithm configurator. In addition to using a highly configured version of clasp, SATFC solves easy instances using a heuristic pre-solving algorithm based on previous partial satisfiable assignments. Finally, some engineering was required to make the whole pipeline as efficient as possible.

### Efficient Usage

SATFC was developed to work particularly well on the auction designs that are being studied by the FCC. For example, in these designs, one expects to encounter many problems to be solved sequentially, a good fraction of which are simple. SATFC thus extensively leverages any partial feasible assignments to get rid of easy instances quickly, and has a main solver tuned for a specific (empirically defined) distribution of harder instances.

## Usage

### Setting Up

Packaged with SATFC are its source code, the necessary libraries, execution scripts as well as a copy of clasp. The latter needs to be compiled on your machine:

```
cd satfc-v1.x.xa-release-x/clasp/
bash compile.sh
```

### Standalone

* **Warning:** Every time SATFC is launched from the command line, it will have to load the Java Virtual Machine, necessary libraries as well as any constraint data corresponding to the specified problem. This is a significant overhead compared to the time required to solve easy instances. If it is necessary to solve large numbers of instances, many of which are easy, we suggest using SATFC as a Java library.

To use SATFC from the command line to solve feasibility checking problems, go in the SATFC directory and execute the following:

```
bash satfc -DATA-FOLDERNAME <data folder> -DOMAINS <station domains>
```

where
* `data folder` points to a folder containing the _broadcasting interference constraints data_ (`domains.csv` and `interferences.csv` files) - see below for further information, and
* `station domains` is a string listing a set of stations to be packed, and for each station a set of eligible channels. This _domains string_ consists of _single station domains strings_ joined by ';', where each single domain string consists of a station numerical ID, a ':', and a list of integer channels joined by ','.  
  For example, the problem of packing stations 1 and 2 into channels 14,15,16,17,18,19,20 and station 3 into channels 14,15,16 would be specified by the following string:  

  ```
  1:14,15,16,17,18,19,20;2:14,15,16,17,18,19,20;3:14,15,16
  ```

One can also run `bash satfc --help` to get a list of the SATFC options and parameters.

#### Data

The broadcast interference constraint data (as specified by the FCC) consists of two files, one specifying station domains, and one specifying pairwise interference between stations. As one of its required arguments, command-line SATFC expects a path to a folder containing both of these files, named `domains.csv` and `interferences.csv`, respectively.

##### Domains

The domains file consists of a CSV with no header where each row encodes a station’s domain (the list of channels on which a station can broadcast). The first entry of a row is always the `DOMAIN` keyword, the second entry is the station ID, and all subsequent entries are domain channels. Note that SATFC uses this file to define the set of available stations (so it should contain all the station IDs that will be used in defining problems).

##### Interferences

The interferences file consists of a CSV with no header where each row is a representation of many pairwise interference constraints between a single subject station on a single subject channels with possibly many target stations. Specifically, the entries of a row are, in order, a key indicating the type of constraint, a subject and target channel on which the constraint applies, the subject station of the constraint, and then a list of target stations to which the constraint applies. Here it is in more compact format:

```
<key>,<subject channel>,<target channel>,<subject station>,<target station 1>,...
```

There are three possible constraint keys: CO, ADJ+1 and ADJ-1. The former indicates a _co-channel constraint_, stating that the subject station and any target station cannot be on the same target/subject channel. The second describes an _adjacent plus one constraint_ which implies that the subject station cannot be on its subject channel c together with any target station on the target channel c + 1. The third one is an _adjacent minus one constraint_, which is just an adjacent plus one constraint with subject station and channels interchanged with target station and channel, respectively.

Here are examples of interference constraints:
* The constraint `CO,4,4,1,2,3` means that neither stations 1 and 2 nor stations 1 and 3 can be jointly assigned to channel 4
* The constraint `ADJ+1,8,9,1,2,3` implies that neither stations 1 and 2 nor stations 1 and 3 can be on channels 8 and 9 respectively.
* The constraint `ADJ-1,8,7,1,2,3` implies that neither stations 1 and 2 nor stations 1 and 3 can be on channels 8 and 7 respectively.

#### Parameters & Options

In addition to the required arguments discussed above, command-line SATFC also exposes the following optional parameters:
* `--help` – display SATFC options and parameters, with short descriptions and helpful information.
* `-PREVIOUS-ASSIGNMENT` – a partial, previously satisfiable channel assignment. This is passed in a string similar to the -DOMAINS string: the station and previous channel pairs are separated by ':', and the different pairs are joined by ','. For example, "1:14,15,16;2:14,15" means pack station 1 into channels 14,15 and 16 and station 2 into channels 14 and 15.
* `-CUTOFF` – a cutoff time, in seconds, for the SATFC execution.
  * **Warning:** SATFC was optimized for runtimes of about one minute. Thus, components might not interact in the most efficient way if cutoff times vastly different than a minute are enforced, especially much shorter ones. Moreover, cutoff times below a second may not always be respected.
* `-SEED` – the seed to use for any (non-clasp ) randomization done in SATFC .
* `-CLASP-LIBRARY` – a path to the compiled ".so" clasp library to use.
* `--log-level` – SATFC's logging level. Can be one of `ERROR, WARN, INFO, DEBUG, TRACE` (listed in increasing order of verbosity).

### As a Library

The most efficient way of using SATFC is as a Java library. This source code is packaged with SATFC's release. The code is well documented; the simplest entry point is the `SATFCFacade` object, and its corresponding builder `SATFCFacadeBuilder`. The reader may find it helpful to consult the Standalone and Data sectiond above to understand the components at play.

## Acknowledgements

Steve Ramage deserves a special mention as he is indirectly responsible for much of SATFC's quality, via his package AEATK, one of the main libraries in SATFC that was used throughout its development for various prototypes, as well as different other miscellaneous features. He also was a constant source of technical help and knowledge when it came to the software design of SATFC.

We would also like to acknowledge various members of the Auctionomics team. Ilya Segal provided the main idea behind the pre-solver used in SATFC. Ulrich Gall and his team members offered much useful feedback after working with SATFC during its early stages.
