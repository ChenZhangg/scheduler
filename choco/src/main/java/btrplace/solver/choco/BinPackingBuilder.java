/*
 * Copyright (c) 2012 University of Nice Sophia-Antipolis
 *
 * This file is part of btrplace.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.solver.choco;

import btrplace.solver.choco.chocoUtil.LightBinPacking;
import choco.cp.solver.CPSolver;
import choco.kernel.solver.ContradictionException;
import choco.kernel.solver.variables.integer.IntDomainVar;

import java.util.ArrayList;
import java.util.List;

/**
 * Builder to create {@link btrplace.solver.choco.chocoUtil.LightBinPacking} constraints
 *
 * @author Fabien Hermenier
 */
public class BinPackingBuilder {

    private ReconfigurationProblem rp;

    private List<IntDomainVar[]> loads;

    private List<IntDomainVar[]> bins;

    private List<IntDomainVar[]> sizes;

    private List<String> names;
    /**
     * Make a new builder.
     *
     * @param rp the associated problem
     */
    public BinPackingBuilder(ReconfigurationProblem rp) {
        this.rp = rp;
        loads = new ArrayList<IntDomainVar[]>();
        bins = new ArrayList<IntDomainVar[]>();
        sizes = new ArrayList<IntDomainVar[]>();
        names = new ArrayList<String>();

    }

    /**
     * Add a dimension.
     *
     * @param loads the resource capacity of each of the nodes
     * @param sizes the resource usage of each of the cSlices
     * @param bins  the resource usage of each of the dSlices
     */
    public void add(String name,IntDomainVar[] loads, IntDomainVar[] sizes, IntDomainVar[] bins) {
        this.loads.add(loads);
        this.sizes.add(sizes);
        this.bins.add(bins);
        this.names.add(name);
    }

    /**
     * Build the constraint.
     *
     * @return the resulting constraint
     */
    public void inject() throws ContradictionException {
        CPSolver solver = rp.getSolver();
        int [][] iSizes = new int[sizes.size()][sizes.get(0).length];
        for (int i = 0; i < sizes.size(); i++) {
            IntDomainVar [] s = sizes.get(i);
            int x = 0;
            for (IntDomainVar ss : s) {
                iSizes[i][x++] = ss.getInf();
                ss.setVal(ss.getInf());
            }

        }
        //TODO: Items must always be in the same order.
        solver.post(new LightBinPacking(names.toArray(new String[names.size()]), solver.getEnvironment(), loads.toArray(new IntDomainVar[loads.size()][]), iSizes, bins.get(0)));
        /*for (int i = 0; i < loads.size(); i++) {
            IntDomainVar[] l = loads.get(i);
            IntDomainVar[] s = sizes.get(i);
            IntDomainVar[] b = bins.get(i);

            int[] iSizes = new int[s.length];
            int x = 0;
            //Instantiate the item sizes to their LB
            for (IntDomainVar ss : s) {
                iSizes[x++] = ss.getInf();
                ss.setVal(ss.getInf());
            }
            solver.post(new LightBinPacking(names.get(i), solver.getEnvironment(), new IntDomainVar[][]{l}, new int[][]{iSizes}, b));
        }         */


    }
}
