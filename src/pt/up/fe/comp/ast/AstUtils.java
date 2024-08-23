package pt.up.fe.comp.ast;

import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Objects;

public abstract class AstUtils {
    public static Type getNodeType(JmmNode node){
        Type type;

        if (Objects.equals(node.get("type"), "intArray")) {
            type = new Type("int", true);
        } else {
            type = new Type(node.get("type"), false);
        }

        return type;
    }

    public static JmmNode getFirstOfKind(JmmNode node, String kind){
        if (node.getKind().equals(kind)){
            return node;
        }

        for(JmmNode child : node.getChildren()){
            if(child.getKind().equals(kind)){
                return child;
            }
            return getFirstOfKind(child, kind);
        }

        return null;
    }
}
