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

package btrplace.safeplace;

import btrplace.model.view.ModelView;
import btrplace.safeplace.verification.CheckerResult;

/**
 * @author Fabien Hermenier
 */
public class CTestCaseResult {

    private long reduceDuration;
    private long testDuration;
    private long preCheckDuration;

    public void setReduceDuration(long reduceDuration) {
        this.reduceDuration = reduceDuration;
    }

    public long getReduceDuration() {
        return reduceDuration;
    }

    public void setTestDuration(long testDuration) {
        this.testDuration = testDuration;
    }

    public long getTestDuration() {
        return testDuration;
    }

    public void setPreCheckDuration(long preCheckDuration) {
        this.preCheckDuration = preCheckDuration;
    }

    public long getPreCheckDuration() {
        return preCheckDuration;
    }

    public static enum Result {success, falsePositive, falseNegative}

    private Result res;

    private String stdout;

    private String stderr;

    private CheckerResult res1, res2;

    private CTestCase tc;

    private long fuzzingDuration;

    public CTestCaseResult(CTestCase tc, CheckerResult r1, CheckerResult r2) {
        stdout = "";
        stderr = "";
        res1 = r1;
        res2 = r2;
        this.tc = tc;
        res = makeResult(res1, res2);
    }

    private Result makeResult(CheckerResult res1, CheckerResult res2) {
        if (res1.getStatus().equals(res2.getStatus())) {
            return CTestCaseResult.Result.success;
        }

        if (res1.getStatus()) {
            return CTestCaseResult.Result.falseNegative;
        }
        return CTestCaseResult.Result.falsePositive;
    }

    public void setStdout(String s) {
        stdout = s;
    }

    public void setStderr(String s) {
        stderr = s;
    }

    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("id: ").append(tc.id()).append("\n");
        b.append("constraint: ").append(tc.getConstraint().toString(tc.getParameters())).append("\n");
        b.append("specRes: ").append(res1).append("\n");
        b.append("vRes: ").append(res2).append("\n");
        b.append("res: ").append(res).append("\n");
        b.append("origin:\n").append(tc.getPlan().getOrigin().getMapping());
        if (!tc.getPlan().getOrigin().getViews().isEmpty()) {
            for (ModelView v : tc.getPlan().getOrigin().getViews()) {
                b.append("view " + v.getIdentifier() + ": " + v + "\n");
            }
        }
        b.append("actions:\n").append(tc.getPlan());
        return b.toString();
    }

    public Result result() {
        return res;
    }

    public CTestCase getTestCase() {
        return tc;
    }

    public String getStdout() {
        return stdout;
    }

    public String getStderr() {
        return stderr;
    }

    public void setFuzzingDuration(long d) {
        fuzzingDuration = d;
    }

    public long getFuzzingDuration() {
        return fuzzingDuration;
    }
}
