package pt.up.fe.comp.analysis;


import java.util.ArrayList;
import java.util.List;

import pt.up.fe.comp.analysis.visitors.SemanticVisitor;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;


public class JmmAnalyser implements JmmAnalysis {
    @Override
    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult) {
        List<Report> reports = new ArrayList<>();
        JmmSymbolTable symbolTable = new JmmSymbolTable();

        JmmNode node = parserResult.getRootNode().sanitize();

        var tableBuilder = new JmmSymbolTableBuilder();
        tableBuilder.visit(parserResult.getRootNode(), symbolTable);

        SemanticVisitor semanticVisitor = new SemanticVisitor(symbolTable);
        semanticVisitor.visit(node, reports);


        reports.addAll(tableBuilder.getReports());

        return new JmmSemanticsResult(parserResult, symbolTable, reports);
    }
}
