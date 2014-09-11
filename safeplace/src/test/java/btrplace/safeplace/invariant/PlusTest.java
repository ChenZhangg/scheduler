/*
 * Copyright (c) 2014 University Nice Sophia Antipolis
 *
 * This file is part of btrplace.
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package btrplace.safeplace.invariant;

import btrplace.safeplace.spec.term.Constant;
import btrplace.safeplace.spec.term.IntPlus;
import btrplace.safeplace.spec.term.Plus;
import btrplace.safeplace.spec.term.SetPlus;
import btrplace.safeplace.spec.type.IntType;
import btrplace.safeplace.spec.type.SetType;
import btrplace.safeplace.spec.type.VMStateType;
import btrplace.safeplace.verification.spec.SpecModel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Fabien Hermenier
 */
public class PlusTest {

    @Test
    public void testInts() {
        Plus p = new IntPlus(IntType.getInstance().newValue("5"), IntType.getInstance().newValue("7"));
        Assert.assertEquals(p.eval(new SpecModel()), 12);
        Assert.assertEquals(p.type(), IntType.getInstance());
    }

    @Test
    public void testCollections() {
        Constant v1 = new Constant(new HashSet(Arrays.asList(1, 2)), new SetType(IntType.getInstance()));
        Constant v2 = new Constant(new HashSet(Arrays.asList(4, 5)), new SetType(IntType.getInstance()));
        Plus p = new SetPlus(v1, v2);
        Set s = (Set) p.eval(new SpecModel());
        Assert.assertEquals(s.size(), 4);
        Assert.assertEquals(p.type(), new SetType(IntType.getInstance()));
    }

    @Test(expectedExceptions = {RuntimeException.class})
    public void testBadCollections() throws RuntimeException {
        Constant v1 = new Constant(new HashSet(Arrays.asList(1, 2)), new SetType(IntType.getInstance()));
        Constant v2 = new Constant(new HashSet(Arrays.asList(VMStateType.getInstance().newValue("running"))), new SetType(VMStateType.getInstance()));
        new SetPlus(v1, v2);
    }

    @Test
    public void testPlusPlus() {
        Plus p1 = new IntPlus(IntType.getInstance().newValue("5"), IntType.getInstance().newValue("7"));
        Plus p2 = new IntPlus(IntType.getInstance().newValue("1"), IntType.getInstance().newValue("2"));
        Plus p3 = new IntPlus(p1, p2);
        Assert.assertEquals(p3.eval(new SpecModel()), 15);
    }

}
