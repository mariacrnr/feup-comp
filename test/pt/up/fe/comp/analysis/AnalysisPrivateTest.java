package pt.up.fe.comp.analysis;

import org.junit.Test;
import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.specs.util.SpecsIo;

public class AnalysisPrivateTest {
    private static JmmSemanticsResult analyseAndPrint(String code) {
        JmmSemanticsResult results = TestUtils.analyse(code);
        System.out.println("SymbolTable: ");
        System.out.println(results.getSymbolTable().print());

        if (results.getSymbolTable() instanceof JmmSymbolTable) {
            ((JmmSymbolTable) results.getSymbolTable()).printLocalVars();
        }

        return results;
    }

    @Test
    public void testChanges() {
        JmmSemanticsResult results = analyseAndPrint(SpecsIo.getResource("fixtures/public/HelloWorld.jmm"));
        System.out.println(results.getRootNode().toTree());
    }

    @Test
    public void testFindMaximum() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/FindMaximum.jmm")));

    }

    @Test
    public void testHelloWorld() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/HelloWorld.jmm")));
    }

    @Test
    public void testLazysort() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/Lazysort.jmm")));
    }

    @Test
    public void testLife() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/Life.jmm")));

    }

    @Test
    public void testMonteCarloPi() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/MonteCarloPi.jmm")));
    }

    @Test
    public void testQuickSort() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/QuickSort.jmm")));
    }

    @Test
    public void testSimple() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/Simple.jmm")));
    }

    @Test
    public void testTicTacToe() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/TicTacToe.jmm")));
    }

    @Test
    public void testWhileAndIf() {
        TestUtils.noErrors(analyseAndPrint(SpecsIo.getResource("fixtures/public/WhileAndIf.jmm")));
    }
}
