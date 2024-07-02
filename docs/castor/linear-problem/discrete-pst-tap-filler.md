# Using integer variables for PST taps

## Used input data

| Name                             | Symbol                                               | Details                                                                                                   |
|----------------------------------|------------------------------------------------------|-----------------------------------------------------------------------------------------------------------|
| PstRangeActions                  | $r \in \mathcal{RA}^{PST}$                           | Set of PST RangeActions                                                                                   |
| reference angle                  | $\alpha _n(r)$                                       | angle of PstRangeAction $r$ at the beginning of the current iteration of the MILP                         |
| reference tap position           | $t_{n}(r)$                                           | tap of PstRangeAction $r$ at the beginning of the current iteration of the MILP                           |
| PstRangeAction angle bounds      | $\underline{\alpha(r)} \: , \: \overline{\alpha(r)}$ | min and max angle[^1] of PstRangeAction $r$                                                               |
| PstRangeAction tap bounds        | $\underline{t(r)} \: , \: \overline{t(r)}$           | min and max tap[^1] of PstRangeAction $r$                                                                 |
| tap-to-angle conversion function | $f_r(t) = \alpha$                                    | Discrete function $f$, which gives, for a given tap of the PstRangeAction $r$, its associated angle value |

[^1]: PST range actions' lower & upper bounds are computed using CRAC + network + previous RAO results, depending on the
types of their ranges: ABSOLUTE, RELATIVE_TO_INITIAL_NETWORK, RELATIVE_TO_PREVIOUS_INSTANT (more
information [here](/input-data/crac/json.md#range-actions))

## Used parameters

| Name                                             | Details                                                                       |
|--------------------------------------------------|-------------------------------------------------------------------------------|
| [pst-model](/parameters.md#pst-model) | This filler is used only if this parameters is set to *APPROXIMATED_INTEGERS* |

## Defined optimization variables

| Name                                         | Symbol             | Details                                                                                                   | Type    | Index                                             | Unit                     | Lower bound | Upper bound |
|----------------------------------------------|--------------------|-----------------------------------------------------------------------------------------------------------|---------|---------------------------------------------------|--------------------------|-------------|-------------|
| PstRangeAction tap upward variation          | $\Delta t^{+} (r)$ | upward tap variation of PstRangeAction $r$, between two iterations of the optimisation                    | Integer | One variable for every element of PstRangeActions | No unit (number of taps) | $-\infty$   | $+\infty$   |
| PstRangeAction tap downward variation        | $\Delta t^{-} (r)$ | downward tap variation of PstRangeAction $r$, between two iterations of the optimisation                  | Integer | One variable for every element of PstRangeActions | No unit (number of taps) | $-\infty$   | $+\infty$   |
| PstRangeAction tap upward variation binary   | $\delta ^{+} (r)$  | indicates whether the tap of PstRangeAction $r$ has increased, between two iterations of the optimisation | Binary  | One variable for every element of PstRangeActions | No unit                  | 0           | 1           |
| PstRangeAction tap downward variation binary | $\delta ^{-} (r)$  | indicates whether the tap of PstRangeAction $r$ has decreased, between two iterations of the optimisation | Binary  | One variable for every element of PstRangeActions | No unit                  | 0           | 1           |

## Used optimization variables

| Name        | Symbol | Defined in                                                                 |
|-------------|--------|----------------------------------------------------------------------------|
| RA setpoint | $A(r)$ | [CoreProblemFiller](core-problem-filler.md#defined-optimization-variables) |

## Defined constraints

### Tap to angle conversion constraint

$$
\begin{equation}
A(r) = \alpha_{n}(r) + c^{+}_{tap \rightarrow a}(r) * \Delta t^{+} (r) - c^{-}_{tap \rightarrow a}(r) * \Delta t^{-} (
r), \forall r \in \mathcal{RA}^{PST}
\end{equation}
$$

<br>

Where the computation of the conversion depends from the context in which the optimization problem is solved.

For the **first solve**, the coefficients are calibrated on the maximum possible variations of the PST:

$$
\begin{equation}
c^{+}_{tap \rightarrow a}(r) = \frac{f_r(\overline{t(r)}) - f_r(t_{n}(r))}{\overline{t(r)} - t_{n}(r)}
\end{equation}
$$

$$
\begin{equation}
c^{-}_{tap \rightarrow a}(r) = \frac{f_r(t_{n}(r)) - f_r(\underline{t(r)})}{t_{n}(r) - \underline{t(r)}}
\end{equation}
$$

For the **second and next solves** (during the iteration of the linear optimization), the coefficients are calibrated on
a small variation of 1 tap:

$$
\begin{equation}
c^{+}_{tap \rightarrow a}(r) = f_r(t_{n}(r) + 1) - f_r(t_{n}(r))
\end{equation}
$$

$$
\begin{equation}
c^{-}_{tap \rightarrow a}(r) = f_r(t_{n}(r)) - f_r(t_{n}(r) - 1)
\end{equation}
$$

<br>

*Note that if $t_n(r)$ is equal to its bound $\overline{t(r)}$ (resp. $\underline{t(r)}$), then the coefficient
$c^{+}_{tap \rightarrow a}(r)$ (resp. $c^{-}_{tap \rightarrow a}(r)$) is set equal to 0 instead.*

<br>

### Tap variation can only be in one direction, upward or downward

$$
\begin{equation}
\Delta t^{+} (r) \leq \delta ^{+} (r) [\overline{t(r)} - t_{n}(r)] , \forall r \in \mathcal{RA}^{PST}
\end{equation}
$$

$$
\begin{equation}
\Delta t^{-} (r) \leq \delta ^{-} (r) [t_{n}(r) - \underline{t(r)}] , \forall r \in \mathcal{RA}^{PST}
\end{equation}
$$

$$
\begin{equation}
\delta ^{+} (r) + \delta ^{-} (r)  \leq 1 , \forall r \in \mathcal{RA}^{PST}
\end{equation}
$$