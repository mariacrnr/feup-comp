package pt.up.fe.comp.optimization;

import pt.up.fe.comp.analysis.JmmMethod;
import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.ast.AstUtils;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class OllirExpressionGenerator extends AJmmVisitor<Boolean, String> {
    private final List<Report> reports;
    private final SymbolTable symbolTable;
    private final String methodSignature;
    private final Map<String, String> config;

    private final int indent;
    private final int[] tempCount;

    private int stmtDepth = 0;

    private final StringBuilder code = new StringBuilder();

    private int getNumSpaces(int indent) {
        return indent * OllirGeneratorUtils.getNumSpaces(config);
    }

    OllirExpressionGenerator(List<Report> reports, SymbolTable symbolTable, int indent, int[] tempCount, String methodSignature) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        this.indent = indent;
        this.tempCount = tempCount;
        this.methodSignature = methodSignature;
        this.config = null;

        addVisits();
    }

    OllirExpressionGenerator(Map<String, String> config, List<Report> reports, SymbolTable symbolTable, int indent, int[] tempCount, String methodSignature) {
        this.reports = reports;
        this.symbolTable = symbolTable;
        this.indent = indent;
        this.tempCount = tempCount;
        this.methodSignature = methodSignature;
        this.config = config;

        addVisits();
    }

    private void addVisits() {
        addVisit("Statement", this::statementVisit);
        addVisit("ReturnExpression", this::returnExpressionVisit);
        addVisit("IDAssignment", this::idAssignmentVisit);
        addVisit("ArrayAssignment", this::arrayAssignmentVisit);
        addVisit("IfStatement", this::ifStatementVisit);
        addVisit("WhileStatement", this::whileStatementVisit);
        addVisit("ScopeStatement", this::scopeStatementVisit);

        addVisit("ParenthesisExpression", this::parenthesisVisit);

        addVisit("BinOp", this::binOpVisit);
        addVisit("UnaryOp", this::unaryOpVisit);

        addVisit("ArrayExpression", this::arrayExpressionVisit);
        addVisit("AccessExpression", this::accessVisit);
        addVisit("Literal", this::literalVisit);
        addVisit("ID", this::idVisit);
        addVisit("_New", this::newVisit);

        setDefaultVisit(this::defaultVisit);
    }

    private String generateTmp(String type) {
        return "tmp" + tempCount[0]++ + "." + OllirGeneratorUtils.toOllirType(type);
    }

    private String generateTmp(Type type) {
        return "tmp" + tempCount[0]++ + "." + OllirGeneratorUtils.toOllirType(type);
    }

    public String getCode() {
        return code.toString();
    }

    private String defaultVisit(JmmNode node, Boolean dummy) {
        return "";
    }

    private String returnExpressionVisit(JmmNode returnNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder returnExpression = new StringBuilder();

        returnExpression.append(" ".repeat(getNumSpaces(indent)));
        returnExpression.append("ret.").append(OllirGeneratorUtils.toOllirType(symbolTable.getReturnType(methodSignature))).append(" ");

        if (returnNode.getJmmChild(0).getKind().equals("BinOp") || returnNode.getJmmChild(0).getKind().equals("UnaryOp")) {
            String tmp = generateTmp(symbolTable.getReturnType(methodSignature));
            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(tmp);
            before.append(" :=." + OllirGeneratorUtils.toOllirType(symbolTable.getReturnType(methodSignature)) + " ");
            before.append(visit(returnNode.getJmmChild(0)));
            before.append(";\n");

            returnExpression.append(tmp);
        } else {
            returnExpression.append(visit(returnNode.getJmmChild(0)));

        }

        returnExpression.append(";\n");
        code.append(before);
        code.append(returnExpression);
        return returnExpression.toString();
    }

    private String statementVisit(JmmNode statementNode, Boolean dummy) {
        stmtDepth++;
        StringBuilder statement = new StringBuilder();

        statement.append(visit(statementNode.getJmmChild(0)));

        stmtDepth--;
        if (stmtDepth == 0) {
            code.append(statement);
        }

        return statement.toString();
    }

    private String idAssignmentVisit(JmmNode assignmentNode, Boolean dummy) {
        StringBuilder assignmentStmt = new StringBuilder();

        Symbol s = ((JmmSymbolTable) symbolTable)
                .getLocalVar(methodSignature, assignmentNode.getJmmChild(0).get("name"));

        if (s == null) {
            s = findSymbol(assignmentNode.getJmmChild(0));

            List<Symbol> a = symbolTable.getFields().stream().filter(field -> field.getName()
                    .equals(assignmentNode.getJmmChild(0).get("name"))).collect(Collectors.toList());

            assignmentStmt.append(" ".repeat(getNumSpaces(indent)));

            String tmp = null;
            if(assignmentNode.getJmmChild(1).getKind().equals("BinOp") ||
                    assignmentNode.getJmmChild(1).getKind().equals("AccessExpression") ||
                    assignmentNode.getJmmChild(1).getKind().equals("CallExpression")){
                String op = visit(assignmentNode.getJmmChild(1), dummy);
                String type = op.substring(op.lastIndexOf(".") + 1);
                tmp = generateTmp(type);

                code.append(" ".repeat(getNumSpaces(indent)));
                code.append(tmp + " :=." + type + " " + op + ";\n");
            }
            else{
                tmp = visit(assignmentNode.getJmmChild(1));
            }


            if(a.isEmpty()){
                String v = visit(assignmentNode.getJmmChild(0), dummy);
                assignmentStmt.append(v + " :=." +v.substring(v.lastIndexOf(".") + 1) +" " + tmp + ";\n");
            }else{
                assignmentStmt.append("putfield(this, " + OllirGeneratorUtils.getCode(s) + ", " +
                        tmp + ").V;\n");
            }

            return assignmentStmt.toString();
        }

        assignmentStmt.append(" ".repeat(getNumSpaces(indent)));
        assignmentStmt.append(s.getName() + "." + OllirGeneratorUtils.toOllirType(s.getType()));
        assignmentStmt.append(" :=." + OllirGeneratorUtils.toOllirType(s.getType()) + " ");
        assignmentStmt.append(visit(assignmentNode.getJmmChild(1)));
        assignmentStmt.append(";\n");

        return assignmentStmt.toString();
    }

    private String arrayAssignmentVisit(JmmNode assignmentNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder assignmentStmt = new StringBuilder();

        Symbol s = findSymbol(assignmentNode.getJmmChild(0));

        assert s != null;
        String idx = "tmp" + tempCount[0]++ + ".i32";
        before.append(" ".repeat(getNumSpaces(indent)));
        before.append(idx + " :=.i32 " + visit(assignmentNode.getJmmChild(1).getJmmChild(0)) + ";\n");

        assignmentStmt.append(" ".repeat(getNumSpaces(indent)));
        assignmentStmt.append(s.getName() + "[" + idx + "].");
        assignmentStmt.append(OllirGeneratorUtils.toOllirType(s.getType().getName()));
        assignmentStmt.append(" :=." + OllirGeneratorUtils.toOllirType(s.getType().getName()) + " ");
        assignmentStmt.append(visit(assignmentNode.getJmmChild(2)));
        assignmentStmt.append(";\n");

        code.append(before);
        return assignmentStmt.toString();
    }


    private String binOpVisit(JmmNode binOpNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder binOpStmt = new StringBuilder();

        String[] op = {"", ""};
        for (int i = 0; i < 2; i++) {
            if (!(binOpNode.getJmmChild(i).getKind().equals("BinOp") ||
                    binOpNode.getJmmChild(i).getKind().equals("AccessExpression") ||
                    binOpNode.getJmmChild(i).getKind().equals("CallExpression") ||
                    binOpNode.getJmmChild(i).getKind().equals("UnaryOp"))) {
                op[i] = visit(binOpNode.getJmmChild(i));
                continue;
            }


            JmmNode child = AstUtils.getFirstOfKind(binOpNode.getJmmChild(i), "Literal");
            String type = "";
            if (child != null) {
                type = OllirGeneratorUtils.toOllirType(child.get("type"));
            } else {
                child = AstUtils.getFirstOfKind(binOpNode.getJmmChild(i), "ID");
                assert child != null;
                Symbol s = findSymbol(child);

                type = OllirGeneratorUtils.toOllirType(s.getType().getName());
            }
            op[i] = generateTmp(type);


            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(op[i] + " :=." + type + " " + visit(binOpNode.getJmmChild(i)) + ";\n");
        }


        String opType = OllirGeneratorUtils.getTypeFromOllirVar(op[0]);
        binOpStmt.append(op[0]);
        switch (binOpNode.get("op")) {
            case "And":
                binOpStmt.append(" &&.")
                        .append(opType);
                break;

            case "Less":
                binOpStmt.append(" <.")
                        .append(opType);
                break;

            case "Add":
                binOpStmt.append(" +.")
                        .append(opType);
                break;

            case "Sub":
                binOpStmt.append(" -.")
                        .append(opType);
                break;

            case "Mult":
                binOpStmt.append(" *.")
                        .append(opType);
                break;

            case "Div":
                binOpStmt.append(" /.")
                        .append(opType);
                break;

            default:
                break;
        }
        binOpStmt.append(" " + op[1]);

        code.append(before);
        return binOpStmt.toString();
    }

    private Symbol findSymbol(JmmNode node) {
        Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, node.get("name"));
        if (s == null) {
            s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, node.get("name"));
            if (s == null) {
                List<Symbol> symbolList = symbolTable.getFields().stream().filter(field -> field.getName().equals(node.get("name"))).collect(Collectors.toList());
                s = symbolList.size() > 0 ? symbolList.get(0) : null;
            }
        }
        return s;
    }

    private String unaryOpVisit(JmmNode unaryNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder unaryOpStmt = new StringBuilder();

        String op;

        if (!(unaryNode.getJmmChild(0).getKind().equals("BinOp") ||
                unaryNode.getJmmChild(0).getKind().equals("AccessExpression") ||
                unaryNode.getJmmChild(0).getKind().equals("CallExpression"))) {
            op = visit(unaryNode.getJmmChild(0));
        } else {
            JmmNode child = AstUtils.getFirstOfKind(unaryNode, "Literal");
            String type = "";
            if (child != null) {
                type = OllirGeneratorUtils.toOllirType(child.get("type"));
            } else {
                child = AstUtils.getFirstOfKind(unaryNode.getJmmChild(0), "ID");
                assert child != null;
                Symbol s = findSymbol(child);

                type = OllirGeneratorUtils.toOllirType(s.getType());
            }
            op = generateTmp(type);


            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(op + " :=." + type + " " + visit(unaryNode.getJmmChild(0)) + ";\n");
        }

        String opType = OllirGeneratorUtils.getTypeFromOllirVar(op);
        switch (unaryNode.get("op")) {
            case "Not":
                unaryOpStmt.append("!.")
                        .append(opType);
                break;

            default:
                break;
        }
        unaryOpStmt.append(" " + op);

        code.append(before);
        return unaryOpStmt.toString();
    }

    private String arrayExpressionVisit(JmmNode arrayNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder arrayStmt = new StringBuilder();

        String expResult;

        expResult = generateTmp("int");

        before.append(" ".repeat(getNumSpaces(indent)));
        before.append(expResult + " :=." + OllirGeneratorUtils.toOllirType("int") + " " + visit(arrayNode.getJmmChild(1)) + ";\n");

        Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, arrayNode.getJmmChild(0).get("name"));
        if (s == null) {
            s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, arrayNode.getJmmChild(0).get("name"));
        }

        arrayStmt.append(s.getName() + "[")
                .append(expResult)
                .append("].")
                .append(OllirGeneratorUtils.toOllirType(s.getType().getName()));

        code.append(before);
        return arrayStmt.toString();
    }

    private String accessVisit(JmmNode accessNode, Boolean dummy) {
        StringBuilder accessStmt = new StringBuilder();
        String after = "";

        if (accessNode.getJmmParent().getKind().equals("Statement")) {
            accessStmt.append(" ".repeat(getNumSpaces(indent)));
            after = ";\n";
        }

        String exp = null;
        String tmp = null;

        if (!(accessNode.getJmmChild(0).getKind().equals("AccessExpression") ||
                accessNode.getJmmChild(0).getKind().equals("CallExpression"))) {
            exp = visit(accessNode.getJmmChild(0));
        } else {
            tmp = visit(accessNode.getJmmChild(0));
            exp = "tmp" + tempCount[0]++ + "." + accessNode.getJmmChild(0).get("ollirType");
            code.append(" ".repeat(getNumSpaces(indent)) + exp +
                    " :=." + accessNode.getJmmChild(0).get("ollirType") + " " + tmp + ";\n");
        }

        if (accessNode.getNumChildren() == 1) {
            accessNode.put("ollirType", ".i32");
            return "arraylength(" + exp + ").i32" + after;
        }

        List<Symbol> parameterSymbols = new ArrayList<>();
        for (JmmNode node : accessNode.getJmmChild(1).getJmmChild(1).getChildren()) {
            if (node.getKind().equals("Literal")) {
                parameterSymbols.add(new Symbol(AstUtils.getNodeType(node), "any"));
            }
            if (node.getKind().equals("ID")) {
                Symbol s = findSymbol(node);
                assert s != null;
                parameterSymbols.add(s);
            }
        }

        List<String> methodSignatures = symbolTable.getMethods();
        List<JmmMethod> methods = new ArrayList<>();
        for (String m : methodSignatures) {
            JmmMethod method = ((JmmSymbolTable) symbolTable).getMethodObject(m);

            if (method.getName().equals(accessNode.getJmmChild(1).getJmmChild(0).get("name"))) {
                if (method.getParameters().size() == parameterSymbols.size()) {
                    boolean same = true;
                    for (int i = 0; i < parameterSymbols.size(); i++) {
                        if (!method.getParameters().get(i).getType().equals(parameterSymbols.get(i).getType())) {
                            same = false;
                            break;
                        }
                    }
                    if (same) {
                        methods.add(method);
                    }
                }
            }
        }

        StringBuilder parameters = new StringBuilder();
        for (JmmNode child : accessNode.getJmmChild(1).getJmmChild(1).getChildren()) {
            parameters.append(", ");
            if (child.getKind().equals("BinOp") || child.getKind().equals("UnaryOp") ||
                    child.getKind().equals("ArrayExpression") || (child.getKind().equals("AccessExpression") && child.getNumChildren() == 1)) {
                String op = visit(child, dummy);
                String opType = null;

                if (child.getKind().equals("UnaryOp") ||
                        (child.getKind().equals("BinOp") &&
                                (child.get("op").equals("And") || child.get("op").equals("Less")))) {
                    opType = "boolean";
                } else {
                    opType = "int";
                }

                tmp = generateTmp(opType);
                code.append(" ".repeat(getNumSpaces(indent)));
                code.append(tmp + ":=." + OllirGeneratorUtils.toOllirType(opType) + " " + op + ";\n");
                parameters.append(tmp);
            } else {
                if(child.getKind().equals("AccessExpression")){
                    String op = visit(child, dummy);
                    String opType = op.substring(op.lastIndexOf(".") + 1);

                    tmp = generateTmp(opType);
                    code.append(" ".repeat(getNumSpaces(indent)));
                    code.append(tmp + ":=." + OllirGeneratorUtils.toOllirType(opType) + " " + op + ";\n");
                    parameters.append(tmp);
                }else{
                    parameters.append(visit(child, dummy));

                }
            }
        }

        // Invocation type
        // if var in var table -> invokevirtual
        // if imported class -> invokestatic
        String invocation = "";

        if (accessNode.getJmmChild(0).getKind().equals("Literal")) {
            invocation = "invokevirtual";
        } else {
            Symbol variableWithMethod = findSymbol(accessNode.getJmmChild(0));
            if (variableWithMethod != null) {
                invocation = "invokevirtual";
            } else {
                if (symbolTable.getImports().contains(accessNode.getJmmChild(0).get("name"))) {
                    invocation = "invokestatic";
                } else {
                    invocation = "invokestatic";
                }
            }
        }


        if (methods.size() == 0) {
            Optional<JmmNode> ancestor = accessNode.getAncestor("IDAssignment");
            if (ancestor.isPresent()) {
                Symbol variable = findSymbol(ancestor.get().getJmmChild(0));
                assert variable != null;
                String type = OllirGeneratorUtils.toOllirType(variable.getType());

                accessNode.put("ollirType", type);
                accessStmt.append(invocation + "(" +
                        exp + ", \"" +
                        accessNode.getJmmChild(1).getJmmChild(0).get("name") + "\"" + parameters + ")." + type);

                return accessStmt + after;
            }

            accessNode.put("ollirType", ".V");
            accessStmt.append(invocation + "(" +
                    exp + ", \"" +
                    accessNode.getJmmChild(1).getJmmChild(0).get("name") + "\"" + parameters + ").V");

            return accessStmt + after;
        }

        if (methods.size() == 1) {
            String type = OllirGeneratorUtils.toOllirType(methods.get(0).getReturnType());
            accessNode.put("ollirType", type);
            accessStmt.append(invocation + "(" +
                    exp + ", \"" +
                    accessNode.getJmmChild(1).getJmmChild(0).get("name") + "\"" +
                    parameters + ")." + type);
            return accessStmt + after;
        }
        return "";
    }


    private String parenthesisVisit(JmmNode parenthesisNode, Boolean dummy) {
        String op = visit(parenthesisNode.getJmmChild(0));
        String type = op.substring(op.lastIndexOf(".") + 1);
        String tmp = generateTmp(type);
        code.append(" ".repeat(getNumSpaces(indent)));
        code.append(tmp + ":=." + type + " " + op + ";\n");
        return tmp;
    }

    private String scopeStatementVisit(JmmNode scopeNode, Boolean dummy) {
        StringBuilder scopeStmt = new StringBuilder();

        for (JmmNode child : scopeNode.getChildren()) {
            scopeStmt.append(visit(child));
        }

        return scopeStmt.toString();
    }

    private String ifStatementVisit(JmmNode ifNode, Boolean dummy) {
        StringBuilder ifStmt = new StringBuilder();

        String then = "Then" + tempCount[0]++;
        String after = "After" + tempCount[0]++;
        String tmp = generateTmp("boolean");

        ifStmt.append(" ".repeat(getNumSpaces(indent)));
        ifStmt.append(tmp + " :=.bool " + visit(ifNode.getJmmChild(0).getJmmChild(0)) + ";\n");

        ifStmt.append(" ".repeat(getNumSpaces(indent)));
        ifStmt.append("if (" + tmp + ") goto " + then + ";\n");


        ifStmt.append(visit(ifNode.getJmmChild(2).getJmmChild(0)));
        ifStmt.append(" ".repeat(getNumSpaces(indent)));

        ifStmt.append("goto " + after + ";\n");

        ifStmt.append(" ".repeat(getNumSpaces(indent - 1)));
        ifStmt.append(then + ":\n");
        ifStmt.append(visit(ifNode.getJmmChild(1).getJmmChild(0)));
        ifStmt.append(" ".repeat(getNumSpaces(indent - 1)));
        ifStmt.append(after + ":\n");

        return ifStmt.toString();
    }

    private String whileStatementVisit(JmmNode whileNode, Boolean dummy) {
        StringBuilder whileStmt = new StringBuilder();

        String loop = "Loop" + tempCount[0]++;
        String body = "Body" + tempCount[0]++;
        String end = "EndLoop" + tempCount[0]++;
        String tmp = generateTmp("boolean");

        whileStmt.append(" ".repeat(getNumSpaces(indent - 1)));
        whileStmt.append(loop + ":\n");
        whileStmt.append(" ".repeat(getNumSpaces(indent)));
        whileStmt.append(tmp + " :=.bool " + visit(whileNode.getJmmChild(0).getJmmChild(0)) + ";\n");
        whileStmt.append(" ".repeat(getNumSpaces(indent)));
        whileStmt.append("if (" + tmp + ") goto " + body + ";\n");
        whileStmt.append(" ".repeat(getNumSpaces(indent)));
        whileStmt.append("goto " + end + ";\n");
        whileStmt.append(" ".repeat(getNumSpaces(indent - 1)));
        whileStmt.append(body + ":\n");
        whileStmt.append(visit(whileNode.getJmmChild(1).getJmmChild(0)));
        whileStmt.append(" ".repeat(getNumSpaces(indent)));
        whileStmt.append("goto " + loop + ";\n");
        whileStmt.append(" ".repeat(getNumSpaces(indent - 1)));
        whileStmt.append(end + ":\n");

        return whileStmt.toString();
    }

    private String literalVisit(JmmNode literalNode, Boolean dummy) {
        return OllirGeneratorUtils.getCodeLiteral(symbolTable, literalNode);
    }

    private String newVisit(JmmNode newNode, Boolean dummy) {
        StringBuilder before = new StringBuilder();
        StringBuilder newStmt = new StringBuilder();

        if (newNode.get("type").equals("intArray")) {
            String tmp = generateTmp(newNode.get("type"));

            newStmt.append(tmp);

            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(tmp)
                    .append(" :=.")
                    .append(OllirGeneratorUtils.toOllirType(newNode.get("type")));
            before.append(" new(");
            before.append("array, ");
            before.append(visit(newNode.getJmmChild(0)));
            before.append(").array.i32");
            before.append(";\n");
        } else {
            String tmp = generateTmp(newNode.getJmmChild(0).get("type"));

            newStmt.append(tmp);

            before.append(" ".repeat(getNumSpaces(indent)));
            before.append(tmp)
                    .append(" :=.")
                    .append(OllirGeneratorUtils.toOllirType(newNode.getJmmChild(0).get("type")));
            before.append(" new(");
            before.append(OllirGeneratorUtils.toOllirType(newNode.getJmmChild(0).get("type")));
            before.append(").");
            before.append(OllirGeneratorUtils.toOllirType(newNode.getJmmChild(0).get("type")));
            before.append(";\n");

            before.append(" ".repeat(getNumSpaces(indent)));
            before.append("invokespecial(")
                    .append(newStmt)
                    .append(", \"<init>\").V;\n");
        }

        code.append(before);
        return newStmt.toString();
    }

    private String idVisit(JmmNode idNode, Boolean dummy) {

        Symbol s = ((JmmSymbolTable) symbolTable).getLocalVar(methodSignature, idNode.get("name"));

        if (s == null) {
            s = ((JmmSymbolTable) symbolTable).getParameter(methodSignature, idNode.get("name"));

            if (s == null) {
                List<Symbol> fields = symbolTable.getFields().stream().filter(field -> field.getName().equals(idNode.get("name"))).collect(Collectors.toList());
                if (fields.isEmpty()) {
                    List<String[]> importsA = symbolTable.getImports().stream().map(importStmt -> importStmt.split("\\.")).collect(Collectors.toList());
                    List<String> importsB = importsA.stream().map(imports -> imports.length > 1 ? imports[imports.length - 1] : imports[0]).collect(Collectors.toList());

                    if (importsB.contains(idNode.get("name"))) {
                        return idNode.get("name");
                    }

                    reports.add(new Report(ReportType.ERROR, Stage.OPTIMIZATION, Integer.parseInt(idNode.get("column")), "Variable " + idNode.get("name") + " not found"));

                    return "";
                }

                s = fields.get(0);
                String tmp = generateTmp(s.getType());
                code.append(" ".repeat(getNumSpaces(indent)));
                code.append(tmp + " :=." + OllirGeneratorUtils.toOllirType(s.getType()) + " " + "getfield(this, " + OllirGeneratorUtils.getCode(s) + ")." + OllirGeneratorUtils.toOllirType(s.getType()) + ";\n");

                return tmp;
            }

            int idx = symbolTable.getParameters(methodSignature).indexOf(s);

            if (JmmSymbolTable.isMain(methodSignature)) {
                return "$" + idx + "." + idNode.get("name") + "." + OllirGeneratorUtils.toOllirType(s.getType());
            }

            return "$" + ++idx + "." + OllirGeneratorUtils.getCode(s);
        }

        return OllirGeneratorUtils.getCode(s);
    }
}
