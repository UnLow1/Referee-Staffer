# Referee Staffer

[![Build Status](https://travis-ci.com/UnLow1/Referee-Staffer.svg?branch=master)](https://travis-ci.com/UnLow1/Referee-Staffer)
[![Snyk.io vulnerabilities](https://snyk.io/test/github/UnLow1/Referee-Staffer/badge.svg)](https://app.snyk.io/org/unlow1/projects)
[![codeclimate](https://codeclimate.com/github/UnLow1/Referee-Staffer/badges/gpa.svg)](https://codeclimate.com/github/UnLow1/Referee-Staffer)

![Java CI](https://github.com/UnLow1/Referee-Staffer/workflows/Java%20CI%20with%20Maven/badge.svg)
![node.js CI](https://github.com/UnLow1/Referee-Staffer/workflows/Node.js%20CI/badge.svg)

## How it works

In all formulas

<img src="https://latex.codecogs.com/svg.latex?\alpha, \beta, \gamma, \delta, \epsilon - constants" />

### Referee's potential

<img src="https://latex.codecogs.com/svg.latex?P_{i}^{q} = \alpha \frac{\sum_{j=1}^{n_{i}} G_{i}^{j}}{n_{i}} + \beta E_{i}^{q-1}" /> 

where

<img src="https://latex.codecogs.com/svg.latex?0 <= {n_i} < q" /><br>
<img src="https://latex.codecogs.com/svg.latex?P_{i}^{q}\text{ - potential of referee $i$ in queue $q$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?n_{i}\text{ - number of grades from observers received by referee $i$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?G_{i}^{j}\text{ - grade $j$ of referee $i$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?E_{i}^{q-1}\text{ - number of all matches refereed by referee $i$ until queue $q-1$}" />

### Match's difficulty

[//]: # (TODO is alpha needed?)
<img src="https://latex.codecogs.com/svg.latex?D_{i}^{q} = \alpha (\beta - |P_{i}^{q-1}|) + \gamma C_{i} + \delta T_{i}^{q-1} + \epsilon L_{i}^{q-1}" />

where

<img src="https://latex.codecogs.com/svg.latex?D_{i}^{q}\text{ - difficulty of match $i$ in queue $q$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?P_{i}^{q-1}\text{ - points difference between teams in match $i$ after queue $q-1$}" /><br>
<img src="https://latex.codecogs.com/svg.image?C_i = \left\{\begin{matrix}1 & \text{teams in match $i$ are from the same city} \\0 & \text{in other case} \end{matrix}\right." /><br>
<img src="https://latex.codecogs.com/svg.image?T_{i}^{q-1} = \left\{\begin{matrix}1 & \text{teams in match $i$ are in the top 3 in standings after queue $q-1$} \\0 & \text{in other case} \end{matrix}\right." /><br>
<img src="https://latex.codecogs.com/svg.image?L_{i}^{q-1} = \left\{\begin{matrix}1 & \text{teams in match $i$ are in the last 3 in standings after queue $q-1$} \\0 & \text{in other case} \end{matrix}\right." />

### Referee's effective value

[//]: # (TODO maybe sum H and G and get rid off one constant)
<img src="https://latex.codecogs.com/svg.image?E_{i}^{q} = P_{i}^{q} - \alpha C_{i}^{q-1} - \beta H_{i}^{q-1} - \gamma G_{i}^{q-1}" />

where

<img src="https://latex.codecogs.com/svg.image?E_{i}^{q}\text{ - effective value of referee $i$ in queue $q$}" /><br>
<img src="https://latex.codecogs.com/svg.image?P_{i}^{q}\text{ - potential of referee $i$ in queue $q$}" /><br>
<img src="https://latex.codecogs.com/svg.image?C_{i}^{q-1}\text{ - number of matches refereed by referee $i$ until queue $q-1$}" /><br>
<img src="https://latex.codecogs.com/svg.image?H_{i}^{q-1}\text{ - number of home team matches to be refereed by referee $i$ until queue $q-1$}" /><br>
<img src="https://latex.codecogs.com/svg.image?G_{i}^{q-1}\text{ - number of guest team matches to be refereed by referee $i$ until queue $q-1$}" />

## Sample screenshots

### Adding data (referees, teams, matches)

![](data/screenshots/addReferee.png)

### Changing configuration

![](data/screenshots/configuration.png)

### List of matches

![](data/screenshots/listOfMatches.png)

### Staffer

![](data/screenshots/staffer.png)