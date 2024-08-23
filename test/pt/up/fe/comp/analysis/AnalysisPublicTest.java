package pt.up.fe.comp.analysis;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisPublicTest {
    private static JmmSemanticsResult analyseAndPrint(String file) {
        System.out.println("File: " + file);
        System.out.println("Reports: ");
        System.out.println(TestUtils.analyse(SpecsIo.getResource(file)).getReports());

        return TestUtils.analyse(SpecsIo.getResource(file));
    }

    @Test
    public void testFindMaximum() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/FindMaximum.jmm"));
    }

    @Test
    public void testHelloWorld() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/HelloWorld.jmm"));
    }

    @Test
    public void testLazysort() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/Lazysort.jmm"));
    }

    @Test
    public void testLife() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/Life.jmm"));
    }

    @Test
    public void testMonteCarloPi() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/MonteCarloPi.jmm"));
    }

    @Test
    public void testQuickSort() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/QuickSort.jmm"));
    }

    @Test
    public void testSimple() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/Simple.jmm"));
    }

    @Test
    public void testTicTacToe() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/TicTacToe.jmm"));
    }

    @Test
    public void testWhileAndIf() {
        TestUtils.noErrors(analyseAndPrint("fixtures/public/WhileAndIf.jmm"));
    }

    @Test
    public void testArrIndexNotInt() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/arr_index_not_int.jmm"));
    }

    @Test
    public void testArrSizeNotInt() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/arr_size_not_int.jmm"));
    }

    @Test
    public void testBadArguments() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/badArguments.jmm"));
    }

    @Test
    public void testBinopIncomp() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/binop_incomp.jmm"));
    }

    @Test
    public void testFuncNotFound() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/funcNotFound.jmm"));
    }

    @Test
    public void testSimpleLength() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/simple_length.jmm"));
    }

    @Test
    public void testVarExpIncomp() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/var_exp_incomp.jmm"));
    }

    @Test
    public void testVarLitIncomp() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/var_lit_incomp.jmm"));
    }

    @Test
    public void testVarUndef() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/var_undef.jmm"));
    }

    @Test
    public void testVarNotInit() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/varNotInit.jmm"));
    }

    @Test
    public void testExtraMissType() {
        TestUtils.mustFail(analyseAndPrint("fixtures/public/fail/semantic/extra/miss_type.jmm"));
    }
}
