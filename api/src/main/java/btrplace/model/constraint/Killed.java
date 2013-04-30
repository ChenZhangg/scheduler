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

package btrplace.model.constraint;

import btrplace.model.Mapping;
import btrplace.model.Model;
import btrplace.model.SatConstraint;
import btrplace.model.constraint.checker.DenyMyVMsActions;
import btrplace.model.constraint.checker.SatConstraintChecker;
import btrplace.plan.event.KillVM;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

/**
 * A constraint to force a set of VMs to be killed.
 * <p/>
 * The restriction provided by the constraint is discrete.
 *
 * @author Fabien Hermenier
 */
public class Killed extends SatConstraint {

    /**
     * Make a new constraint.
     *
     * @param vms the VMs to remove
     */
    public Killed(Set<UUID> vms) {
        super(vms, Collections.<UUID>emptySet(), false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Killed that = (Killed) o;
        return getInvolvedVMs().equals(that.getInvolvedVMs());
    }

    @Override
    public int hashCode() {
        return getInvolvedVMs().hashCode();
    }

    @Override
    public String toString() {
        return new StringBuilder("killed(vms=")
                .append(getInvolvedVMs())
                .append(", discrete")
                .append(")").toString();
    }

    @Override
    public boolean setContinuous(boolean b) {
        return !b;
    }

    @Override
    public SatConstraintChecker getChecker() {
        return new Checker(this);
    }

    private class Checker extends DenyMyVMsActions {

        public Checker(Killed k) {
            super(k);
        }

        @Override
        public boolean start(KillVM a) {
            return true;
        }

        @Override
        public boolean endsWith(Model mo) {
            Mapping c = mo.getMapping();
            for (UUID vm : vms) {
                if (c.getAllVMs().contains(vm)) {
                    return false;
                }
            }
            return true;
        }
    }
}
