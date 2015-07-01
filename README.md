# Feasibility Checker

## Introduction

**SATFC** (_SAT-based Feasibility Checker_) solves radio-spectrum repacking feasibility problems arising in the reverse auction of the FCC's upcoming broadcast incentive auction. It combines a formulation of feasibility checking based on propositional satisfiability with a heuristic pre-solver and a SAT solver tuned for the types of instances observed in auction simulations.
authors & collaborators SATFC is the product of the ideas and hard work of Auctionomics, notably Alexandre Fréchette, Neil Newman, Paul Cernek, Emily Chenn, Guillaume Saulnier-Comte, Nick Arnosti, and Kevin Leyton-Brown.

Questions, bug reports and feature suggestions should be directed to Alexandre Fréchette - afrechet at cs dot ubc dot ca

## Installation
Clone the repository.

A stand-alone, ready-to-go release can be found in `satfc/releases/`.

## Usage
Please consult the manual `SATFC-manual.pdf` located in `satfc/manual/` or packaged with any stand-alone release.

Command line usage, starting from a stand-alone SATFC directory:
```
./bin/SATFC -DATA-FOLDERNAME <interference constraints folder> -DOMAINS <station domains map>
```

To build SATFC from source, starting from the root SATFC project directory:
```
./gradlew installDist
```

## Release Notes

Rough information about the evolution of **SATFC** through releases.

### SATFC 1.7.1 [30/06/2015]

* New required command line parameter for the server, _constraint.folder_, that tells the server where all of the data folders are so that it can properly size cache entries
* Upgrade to clasp 3.1.2

### SATFC 1.7 [8/06/2015]

* New feature, _parallel portfolios_, allow SATFC to execute multiple solvers in parallel, returning a result as soon as any solver succeeds. Previous versions of SATFC applied each solver sequentially.
* Enhanced the presolver technique to look beyond a station's immediate neighbours

### SATFC 1.6 [19/05/2015]

* Upgrade from **clasp** version from 2 to 3, along with new better performing configurations.
* The **SATFCserver** now has a clever clean up procedures to prune away unnecessary entries if the cache becomes too big.
* The cache is now updated on the fly. This means instances solved are immediately accessible to future cache queries (improving performance), and the cache server does not need to be restarted to have access to new data (improving usability). 

### SATFC 1.5 [26/03/2015]

* New feature, _containment cache_, that uses previously solved problems to solve new, related problems. Our experiments with this feature show that it provides significant performance increase, not only by taking care of problems that are seen often (e.g. from very similar auction runs), but also improving solving time of new, unseen problems. This cache is implemented as a standalone server, the **SATFCserver**, to allow multiple **SATFC** solvers to use the same cache and reduce memory overhead. This server in turn uses **redis** as its backbone cache data structure.

### SATFC 1.3 [30/10/2014]

* First official release of **SATFC** by the FCC.

