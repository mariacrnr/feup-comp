package pt.up.fe.comp.optimization;

import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.Map;
import java.util.stream.Collectors;

public abstract class OllirGeneratorUtils {
    private static final int numSpaces = 4;

    public static int getNumSpaces(Map<String, String> config) {
        return numSpaces;
    }

    public static String toOllirType(String type, boolean isArray) {
        StringBuilder code = new StringBuilder();

        if (isArray) {
            code.append("array.");
        }

        switch (type) {
            case "void":
                code.append("V");
                break;
            case "int":
                code.append("i32");
                break;
            case "boolean":
                code.append("bool");
                break;
            default:
                code.append(type);
                break;
        }

        return code.toString();
    }

    public static String toOllirType(Type type){
        return toOllirType(type.getName(), type.isArray());
    }

    public static String toOllirType(String type){
        if(type.equals("intArray")){
            return toOllirType("int", true);
        }

        return type.contains("[") ? toOllirType(type, true) : toOllirType(type, false) ;
    }

    public static String getCode(Symbol symbol) {
        return symbol.getName() + "." + OllirGeneratorUtils.toOllirType(symbol.getType());
    }

    public static String getMethodHeader(JmmSymbolTable symbolTable, String methodSignature){
        JmmMethod method = symbolTable.getMethodObject(methodSignature);
        String params = method.getParameters().stream().map(OllirGeneratorUtils::getCode).collect(Collectors.joining(", "));
        return method.getName() + "(" + params + ")." + toOllirType(method.getReturnType());
    }

    public static String getCodeLiteral(SymbolTable symbolTable, JmmNode literalNode) {
        if (literalNode.get("value").equals("this")) {
            return "this";
        }

        if (literalNode.get("value").equals("true")){
            return 1 + "." + toOllirType(literalNode.get("type"), false);
        }

        if (literalNode.get("value").equals("false")) {
            return 0 + "." + toOllirType(literalNode.get("type"), false);

        }

        return literalNode.get("value") + "." + toOllirType(literalNode.get("type"), false);
    }

    public static String getTypeFromOllirVar(String ollirVar){
        StringBuilder opType = new StringBuilder();
        String vartype = ollirVar.substring(0, ollirVar.contains("]") ? ollirVar.indexOf("]") : ollirVar.length());
        String[] split = vartype.split("\\.");
        for (int i = 0; i < split.length; i++) {
            if (split[i].contains("$")) {
                i++;
                continue;
            }

            if (i == 0) {
                continue;
            }

            opType.append(split[i]);
        }

        return opType.toString();
    }
}
