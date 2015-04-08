# Feasibility Checker

## Introduction

SATFC (_SAT-based Feasibility Checker_) solves radio-spectrum repacking feasibility problems arising in the reverse auction of the FCC's upcoming broadcast incentive auction. It combines a formulation of feasibility checking based on propositional satisfiability with a heuristic pre-solver and a SAT solver tuned for the types of instances observed in auction simulations.
authors & collaborators SATFC is the product of the ideas and hard work of Auctionomics, notably Alexandre Fréchette, Neil Newman, Guillaume Saulnier-Comte, Nick Arnosti, and Kevin Leyton-Brown.

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
./gradlew :satfc:installDist
```
