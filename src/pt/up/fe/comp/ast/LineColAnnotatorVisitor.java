package pt.up.fe.comp.ast;

import pt.up.fe.comp.Node;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;

public class LineColAnnotatorVisitor extends PreorderJmmVisitor<Integer, Integer> {
    public LineColAnnotatorVisitor(){
        setDefaultVisit(this::annotateLineCol);
    }

    private Integer annotateLineCol(JmmNode node, Integer dummy){
        var parserNode = (Node) node;
        node.put("line", Integer.toString(parserNode.getBeginLine()));
        node.put("column", Integer.toString(parserNode.getBeginColumn()));
        return 0;
    }
}