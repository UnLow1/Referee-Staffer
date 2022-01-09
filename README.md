# Referee Staffer

[![Build Status](https://travis-ci.com/UnLow1/Referee-Staffer.svg?branch=master)](https://travis-ci.com/UnLow1/Referee-Staffer)
[![Snyk.io vulnerabilities](https://snyk.io/test/github/UnLow1/Referee-Staffer/badge.svg)](https://app.snyk.io/org/unlow1/projects)
[![codeclimate](https://codeclimate.com/github/UnLow1/Referee-Staffer/badges/gpa.svg)](https://codeclimate.com/github/UnLow1/Referee-Staffer)

![Java CI](https://github.com/UnLow1/Referee-Staffer/workflows/Java%20CI%20with%20Maven/badge.svg)
![node.js CI](https://github.com/UnLow1/Referee-Staffer/workflows/Node.js%20CI/badge.svg)

## How it works

In all formulas

<img src="https://latex.codecogs.com/svg.latex?\alpha,\beta,\gamma,\delta,\epsilon-constants" />

### Referee's potential

<img src="https://latex.codecogs.com/svg.latex?P_{i}^{q}=\alpha\frac{\sum_{j=1}^{n_{i}}G_{i}^{j}}{n_{i}}+\beta%20E_{i}^{q-1}" />

where

<img src="https://latex.codecogs.com/svg.latex?0<={n_i}<q"/><br>
<img src="https://latex.codecogs.com/svg.latex?P_{i}^{q}\text{%20-%20potential%20of%20referee%20$i$%20in%20queue%20$q$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?n_{i}\text{%20-%20number%20of%20grades%20from%20observers%20received%20by%20referee%20$i$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?G_{i}^{j}\text{%20-%20grade%20$j$%20of%20referee%20$i$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?E_{i}^{q-1}\text{%20-%20number%20of%20all%20matches%20refereed%20by%20referee%20$i$%20until%20queue%20$q-1$}" />

### Match's difficulty

[//]: # (TODO is alpha needed?)
<img src="https://latex.codecogs.com/svg.latex?D_{i}^{q}=\alpha(\beta-|P_{i}^{q-1}|)+\gamma%20C_{i}+\delta%20T_{i}^{q-1}+\epsilon%20L_{i}^{q-1}" />

where

<img src="https://latex.codecogs.com/svg.latex?D_{i}^{q}\text{%20-%20difficulty%20of%20match%20$i$%20in%20queue%20$q$}" /><br>
<img src="https://latex.codecogs.com/svg.latex?P_{i}^{q-1}\text{%20-%20points%20difference%20between%20teams%20in%20match%20$i$%20after%20queue%20$q-1$}" /><br>
<img src="https://latex.codecogs.com/svg.image?C_i=\left\{\begin{matrix}1&\text{teams%20in%20match%20$i$%20are%20from%20the%20same%20city}\\0&\text{in%20other%20case}\end{matrix}\right." /><br>
<img src="https://latex.codecogs.com/svg.image?T_{i}^{q-1}=\left\{\begin{matrix}1&\text{teams%20in%20match%20$i$%20are%20in%20the%20top%203%20in%20standings%20after%20queue%20$q-1$}\\0&\text{in%20other%20case}\end{matrix}\right." /><br>
<img src="https://latex.codecogs.com/svg.image?L_{i}^{q-1}=\left\{\begin{matrix}1&\text{teams%20in%20match%20$i$%20are%20in%20the%20last%203%20in%20standings%20after%20queue%20$q-1$}\\0&\text{in%20other%20case}\end{matrix}\right." />

### Referee's effective value

[//]: # (TODO maybe sum H and G and get rid off one constant)
<img src="https://latex.codecogs.com/svg.image?E_{i}^{q}=P_{i}^{q}-\alpha%20C_{i}^{q-1}-\beta%20H_{i}^{q-1}-\gamma%20G_{i}^{q-1}" />

where

<img src="https://latex.codecogs.com/svg.image?E_{i}^{q}\text{%20-%20effective%20value%20of%20referee%20$i$%20in%20queue%20$q$}" /><br>
<img src="https://latex.codecogs.com/svg.image?P_{i}^{q}\text{%20-%20potential%20of%20referee%20$i$%20in%20queue%20$q$}" /><br>
<img src="https://latex.codecogs.com/svg.image?C_{i}^{q-1}\text{%20-%20number%20of%20matches%20refereed%20by%20referee%20$i$%20until%20queue%20$q-1$}" /><br>
<img src="https://latex.codecogs.com/svg.image?H_{i}^{q-1}\text{%20-%20number%20of%20home%20team%20matches%20to%20be%20refereed%20by%20referee%20$i$%20until%20queue%20$q-1$}" /><br>
<img src="https://latex.codecogs.com/svg.image?G_{i}^{q-1}\text{%20-%20number%20of%20guest%20team%20matches%20to%20be%20refereed%20by%20referee%20$i$%20until%20queue%20$q-1$}" />

## Sample screenshots

### Adding data (referees, teams, matches)

![](data/screenshots/addReferee.png)

### Changing configuration

![](data/screenshots/configuration.png)

### List of matches

![](data/screenshots/listOfMatches.png)

### Staffer

![](data/screenshots/staffer.png)