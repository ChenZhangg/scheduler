/*
 * Copyright  2021 The BtrPlace Authors. All rights reserved.
 * Use of this source code is governed by a LGPL-style
 * license that can be found in the LICENSE.txt file.
 */

package org.btrplace.scheduler.choco.extensions;


import org.chocosolver.memory.IStateBitSet;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.delta.IIntDeltaMonitor;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;
import org.chocosolver.util.iterators.DisposableValueIterator;
import org.chocosolver.util.procedure.UnaryIntProcedure;
import org.chocosolver.util.tools.ArrayUtils;

import java.util.BitSet;

/**
 * Enforces multiple sets of variables values to be disjoint
 * created sofdem - 08/09/11
 *
 * @author Sophie Demassey
 */
public class DisjointMultiple extends Constraint {

    /**
     * @param vs sets of variables
     * @param c  max variable value + 1
     */
    public DisjointMultiple(IntVar[][] vs, int c) {
        super("DisjointMultiple", new DisjointPropagator(vs, c));
    }

    static class DisjointPropagator extends Propagator<IntVar> {

        /**
         * the variable domains must be included in [0, nbValues-1]
         */
        private final int nbValues;

        /**
         * the number of groups
         */
        private final int nbGroups;

        /**
         * indices of variables in group 'g' is between groupIdx[g] and groupIdx[g+1]
         * with 0 <= g < nbGroups
         */
        private final int[] groupIdx;

      /**
       * candidates[g][v] = number of variables in group 'g' which can be assigned to value 'v',
       * with 0 <= g < nbGroups and 0 <= v < nbValues
       */
      private final IStateInt[][] candidates;
      /**
       * required[g].get(v) iff at least one variable in group 'g' is assigned to value 'v',
       * with 0 <= g < nbGroups and 0 <= v < nbValues
       */
      private final IStateBitSet[] required;

        private final IIntDeltaMonitor[] idms;

      private boolean first = true;

        private final RemProc remProc;

      /**
       * @param vs sets of variables
         * @param c  max variable value + 1
         */
        public DisjointPropagator(IntVar[][] vs, int c) {
            super(ArrayUtils.flatten(vs), PropagatorPriority.VERY_SLOW, true);
            nbValues = c;
            nbGroups = vs.length;
            groupIdx = new int[nbGroups + 1];
            candidates = new IStateInt[nbGroups][c];
            required = new IStateBitSet[nbGroups];
            groupIdx[0] = 0;
            int idx = 0;
            for (int g = 0; g < nbGroups; g++) {
                idx += vs[g].length;
                groupIdx[g + 1] = idx;
                required[g] = getModel().getEnvironment().makeBitSet(c);
                for (int v = 0; v < c; v++) {
                    candidates[g][v] = getModel().getEnvironment().makeInt(0);
                }
            }

            idms = new IIntDeltaMonitor[vars.length];
            int i = 0;
            for (IntVar v : vars) {
                idms[i++] = v.monitorDelta(this);
            }
            remProc = new RemProc();
        }

        @Override
        public int getPropagationConditions(int vIdx) {
            //TODO: REMOVE should be fine
            return IntEventType.all();
        }

        @Override
        public ESat isEntailed() {
            BitSet valuesOne = new BitSet(nbValues);
            for (int g = 0; g < nbGroups; g++) {
                for (int i = groupIdx[g]; i < groupIdx[g + 1]; i++) {
                    valuesOne.set(vars[i].getValue());
                }
                for (int i = 0; i < groupIdx[g]; i++) {
                    if (valuesOne.get(vars[i].getValue())) {
                        return ESat.FALSE;
                    }
                }
                for (int i = groupIdx[g + 1]; i < groupIdx[nbGroups]; i++) {
                    if (valuesOne.get(vars[i].getValue())) {
                        return ESat.FALSE;
                    }
                }
                valuesOne.clear();
            }
            return ESat.TRUE;
        }

        /**
         * Initialise required and candidate for a given variable that belong to a given group.
         *
         * @param var   the variable
         * @param group the group of the variable
         */
        private void initVar(IntVar var, int group) {
            if (var.isInstantiated()) {
                required[group].set(var.getValue());
            } else {
                DisposableValueIterator it = var.getValueIterator(true);
                try {
                    while (it.hasNext()) {
                        int val = it.next();
                        candidates[group][val].add(1);
                    }
                } finally {
                    it.dispose();
                }
            }
        }

        @Override
        public void propagate(int m) throws ContradictionException {
            if (first) {
                first = false;
                int i = 0;
                for (int g = 0; g < nbGroups; g++) {
                    for (; i < groupIdx[g + 1]; i++) {
                        initVar(vars[i], g);
                    }
                }
            }

            for (int v = 0; v < nbValues; v++) {
                for (int g = 0; g < nbGroups; g++) {
                    if (required[g].get(v)) {
                        setRequired(v, g);
                    }
                }
            }
        }

        @Override
        public void propagate(int idx, int mask) throws ContradictionException {
            if (IntEventType.isInstantiate(mask)) {
                int group = getGroup(idx);
                if (!required[group].get(vars[idx].getValue())) {
                    setRequired(vars[idx].getValue(), group);
                }
            }
            if (IntEventType.isRemove(mask)) {
                idms[idx].forEachRemVal(remProc.set(idx));
            }
        }

        /**
         * update the internal data and filter when a variable is newly instantiated
         * 1) fail if a variable in the other group is already instantiated to this value
         * 2) remove the value of the domains of all the variables of the other group
         *
         * @param val   the new assigned value
         * @param group the group of the new instantiated variable
         * @throws ContradictionException when some variables in both groups are instantiated to the same value
         */
        public void setRequired(int val, int group) throws ContradictionException {
            required[group].set(val);
            for (int g = 0; g < nbGroups; g++) {
                if (g != group) {
                    if (required[g].get(val)) {
                        //The value is used in the other group. It's a contradiction
                        fails();
                    }
                    if (candidates[g][val].get() > 0) {
                        //The value was possible for the other group, so we remove it from its variable
                        for (int i = groupIdx[g]; i < groupIdx[g + 1]; i++) {
                            if (vars[i].removeValue(val, this)) {
                                candidates[g][val].add(-1);
                                if (vars[i].isInstantiated() && !required[g].get(vars[i].getValue())) {
                                    setRequired(vars[i].getValue(), g);
                                }
                            }
                        }
                    }
                }
            }
        }

        private int getGroup(int idx) {
            return getGroup(idx, 0, nbGroups);
        }

        private int getGroup(int idx, int s, int e) {
            assert e > s && groupIdx[s] <= idx && idx < groupIdx[e];
            if (e == s + 1) {
                return s;
            }
            //Complicated average computation that should
            // prevent an overflow from findbug point of view
            int m = (s + e) >>> 1;
            if (idx >= groupIdx[m]) {
                return getGroup(idx, m, e);
            }
            return getGroup(idx, s, m);

        }

        public ESat isSatisfied(int[] tuple) {
            BitSet valuesOne = new BitSet(nbValues);
            for (int g = 0; g < nbGroups; g++) {
                for (int i = groupIdx[g]; i < groupIdx[g + 1]; i++) {
                    valuesOne.set(tuple[i]);
                }
                for (int i = 0; i < groupIdx[g]; i++) {
                    if (valuesOne.get(tuple[i])) {
                        return ESat.FALSE;
                    }
                }
                for (int i = groupIdx[g + 1]; i < groupIdx[nbGroups + 1]; i++) {
                    if (valuesOne.get(tuple[i])) {
                        return ESat.FALSE;
                    }
                }
                valuesOne.clear();
            }
            return ESat.TRUE;
        }


        private class RemProc implements UnaryIntProcedure<Integer> {
            private int group;

            @Override
            public UnaryIntProcedure<Integer> set(Integer idxVar) {
                this.group = getGroup(idxVar);
                return this;
            }

            @Override
            public void execute(int val) throws ContradictionException {
                candidates[group][val].add(-1);
            }
        }
    }
}
