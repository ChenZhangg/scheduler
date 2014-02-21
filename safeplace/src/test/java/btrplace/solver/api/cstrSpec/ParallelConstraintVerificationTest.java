package btrplace.solver.api.cstrSpec;

import btrplace.solver.api.cstrSpec.fuzzer.Fuzzer;
import btrplace.solver.api.cstrSpec.spec.SpecReader;
import btrplace.solver.api.cstrSpec.spec.term.Constant;
import btrplace.solver.api.cstrSpec.verification.TestCase;
import btrplace.solver.api.cstrSpec.verification.Verifier;
import btrplace.solver.api.cstrSpec.verification.btrplace.ImplVerifier;
import btrplace.solver.api.cstrSpec.verification.spec.SpecVerifier;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class ParallelConstraintVerificationTest {

    public Specification getSpec() throws Exception {
        SpecReader r = new SpecReader();
        return r.getSpecification(new File("src/test/resources/v1.cspec"));
    }

    @Test
    public void test() throws Exception {

        Specification s = getSpec();
        List<Constraint> cores = new ArrayList<>();
        for (Constraint c : s.getConstraints()) {
            if (c.isCore()) {
                cores.add(c);
            }
        }
        Fuzzer fuzzer = new Fuzzer(1, 1).minDuration(1).maxDuration(3).allDurations().allDelays();

        List<Verifier> verifs = Arrays.asList(new SpecVerifier(), new ImplVerifier());
        Constraint c = s.get("noVMsOnOfflineNodes");
        System.out.println(c.pretty());
        ParallelConstraintVerification pc = new ParallelConstraintVerification(fuzzer, verifs, 4, c, Collections.<Constant>emptyList(), false);
        List<TestCase> issues = pc.verify();
        Assert.assertEquals(issues.size(), 12);
    }
}
