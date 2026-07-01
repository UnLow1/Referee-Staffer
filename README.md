# Referee Staffer

[![codecov](https://codecov.io/gh/UnLow1/Referee-Staffer/branch/master/graph/badge.svg)](https://codecov.io/gh/UnLow1/Referee-Staffer)
[![Snyk.io vulnerabilities](https://snyk.io/test/github/UnLow1/Referee-Staffer/badge.svg)](https://app.snyk.io/org/unlow1/projects)
[![codeclimate](https://codeclimate.com/github/UnLow1/Referee-Staffer/badges/gpa.svg)](https://codeclimate.com/github/UnLow1/Referee-Staffer)

[![Backend CI (Maven)](https://github.com/UnLow1/Referee-Staffer/actions/workflows/maven.yml/badge.svg)](https://github.com/UnLow1/Referee-Staffer/actions/workflows/maven.yml)
[![Frontend CI (Angular)](https://github.com/UnLow1/Referee-Staffer/actions/workflows/frontend.yml/badge.svg)](https://github.com/UnLow1/Referee-Staffer/actions/workflows/frontend.yml)
[![CodeQL](https://github.com/UnLow1/Referee-Staffer/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/UnLow1/Referee-Staffer/actions/workflows/codeql-analysis.yml)

## How it works

In all formulas $\alpha, \beta, \gamma, \delta, \epsilon$ - constants

### Referee's potential

$$P_{i}^{q} = \alpha \frac{\sum_{j=1}^{n_{i}} G_{i}^{j}}{n_{i}} + \beta E_{i}^{q-1}$$

where

$0 \leq n_{i} < q$ <br>
$P_{i}^{q}$ - potential of referee $i$ in queue $q$ <br>
$n_{i}$ - number of grades from observers received by referee $i$ <br>
$G_{i}^{j}$ - grade $j$ of referee $i$ <br>
$E_{i}^{q-1}$ - number of all matches refereed by referee $i$ until queue $q-1$

### Match's difficulty

[//]: # (TODO is alpha needed?)

$$D_{i}^{q} = \alpha (\beta - |P_{i}^{q-1}|) + \gamma C_{i} + \delta T_{i}^{q-1} + \epsilon L_{i}^{q-1}$$

where

$D_{i}^{q}$ - difficulty of match $i$ in queue $q$ <br>
$P_{i}^{q-1}$ - points difference between teams in match $i$ after queue $q-1$ <br>
$$C_{i} = \begin{cases} 1 & \text{teams in match } i \text{ are from the same city} \\ 0 & \text{in other case} \end{cases}$$

$$T_{i}^{q-1} = \begin{cases} 1 & \text{teams in match } i \text{ are in the top 3 in standings after queue } q-1 \\ 0 & \text{in other case} \end{cases}$$

$$L_{i}^{q-1} = \begin{cases} 1 & \text{teams in match } i \text{ are in the last 3 in standings after queue } q-1 \\ 0 & \text{in other case} \end{cases}$$

### Referee's effective value

[//]: # (TODO maybe sum H and G and get rid off one constant)

$$E_{i}^{q} = P_{i}^{q} - \alpha C_{i}^{q-1} - \beta H_{i}^{q-1} - \gamma G_{i}^{q-1}$$

where

$E_{i}^{q}$ - effective value of referee $i$ in queue $q$ <br>
$P_{i}^{q}$ - potential of referee $i$ in queue $q$ <br>
$C_{i}^{q-1}$ - number of matches refereed by referee $i$ until queue $q-1$ <br>
$H_{i}^{q-1}$ - number of home team matches to be refereed by referee $i$ until queue $q-1$ <br>
$G_{i}^{q-1}$ - number of guest team matches to be refereed by referee $i$ until queue $q-1$

## Sample screenshots

### Overview dashboard

![](data/screenshots/dashboard.png)

### Staffer

The scoring-formula panel ("Algorithm explainer") can be toggled from the sidebar's Admin section.

![](data/screenshots/staffer.png)

### List of matches

![](data/screenshots/listOfMatches.png)

### Adding data (referees, teams, matches)

Add/edit forms open as right-side drawers on top of their lists.

![](data/screenshots/addReferee.png)

### Changing configuration

![](data/screenshots/configuration.png)

### Admin panel

The Admin nav group (Teams · Standings · Vacations) is hidden by default. Reveal it with the
"Show admin section" button at the bottom of the sidebar — a temporary stand-in until real
authorization lands.

![](data/screenshots/adminPanel.png)

### Dark mode

Toggle with the sun/moon button in the top bar.

![](data/screenshots/darkMode.png)
