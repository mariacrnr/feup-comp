package pt.up.fe.comp.analysis.visitors;

import pt.up.fe.comp.analysis.JmmSymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.analysis.JmmMethod;

import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class SemanticVisitor extends AJmmVisitor<List<Report>, String> {
    JmmSymbolTable symbolTable;
    List<String> variables;

    public SemanticVisitor(JmmSymbolTable symbolTable){
        this.symbolTable = symbolTable;
        this.variables = new ArrayList<>();
        //methods
        addVisit("MethodBody", this::visitMethodBody);
        addVisit("MethodHeader", this::visitMethodHeader);
        addVisit("MemberArgs", this::visitMemberArgs);
        //expressions
        addVisit("ArrayExpression", this::visitArrayExpression);
        addVisit("AccessExpression", this::visitAccessExpression);
        addVisit("CallExpression", this::visitCallExpression);
        addVisit("ParenthesisExpression", this::visitParenthesisExpression);
        //conditions
        addVisit("IfCondition", this::visitCondition);
        addVisit("WhileCondition", this::visitCondition);
        //ops
        addVisit("UnaryOp", this::visitUnaryOp);
        addVisit("BinOp", this::visitBinOp);
        //ID
        addVisit("IDAssignment", this::visitIDAssignment);
        addVisit("ID", this::visitID);

        addVisit("VarDeclaration", this::visitVarDeclaration);
        addVisit("_New", this::visitNew);
        addVisit("Type", this::visitType);
        addVisit("Literal", this::visitLiteral);

        setDefaultVisit(this::defaultVisit);
    }

    /**
     * Visitors
     */
    private String defaultVisit(JmmNode node, List<Report> reports){
        for (JmmNode child : node.getChildren())
            visit(child, reports);
        return "";
    }


    private String visitType(JmmNode node, List<Report> reports){
        return node.get("type");
    }

    private String visitLiteral(JmmNode node, List<Report> reports){
        return node.get("type");
    }

    private String visitNew(JmmNode node, List<Report> reports){
        String newTypeArray = node.get("type");

        String newType = newTypeArray.substring(0, newTypeArray.length() - 5);

        String childType = visit(node.getJmmChild(0), reports);

        if (newTypeArray.equals("Class")) return childType;

        if (childType.equals(newType)) return newTypeArray + "Expression";

        addSemanticErrorReport(reports, Integer.parseInt(node.get("line")),
                Integer.parseInt(node.get("column")),
                "Array length must be of type int.");

        return "<Invalid>";
    }

    private String visitVarDeclaration(JmmNode node, List<Report> reports){
        List<String> types = new ArrayList<>(Arrays.asList("int", "intArray", "boolean", symbolTable.getClassName()));

        for (String imp: symbolTable.getImports()) {
            String[] impList = imp.split("\\.");
            String type = impList[impList.length-1];
            types.add(type);
        }

        if (!types.contains(node.getJmmChild(0).get("type"))){
            addSemanticErrorReport(reports, Integer.parseInt(node.getJmmChild(0).get("line")),
                    Integer.parseInt(node.getJmmChild(0).get("column")),
                    "Type \"" + node.getJmmChild(0).get("type") + "\" is not defined.");
            return "<Invalid>";
        }

        return visit(node.getJmmChild(0), reports);
    }

    /**
     * ID Visitors
     */
    private String visitIDAssignment(JmmNode node, List<Report> reports){
        JmmNode id0 = node.getJmmChild(0);
        JmmNode id1 = node.getJmmChild(1);

        if (!id0.getKind().equals("ID"))
            addSemanticErrorReport(reports, id0, id0.getKind() + " is not an ID");

        String type0 = visit(id0, reports);
        Symbol s0 = symbolTable.getFieldByName(id0.get("name"));

        if (s0 != null && type0.equals("<Invalid>")) {
            if (node.getAncestor("MainMethod").isPresent()){
                addSemanticErrorReport(reports, id1,
                        "Static method can't assign value to class field.");
                return "<Invalid>";
            }
            type0 = s0.getType().getName();
        }

        String type1 = visit(id1, reports);

        JmmMethod method = symbolTable.getParentMethodName(node);
        if (type1.equals("<Invalid>"))
            if (id1.getAttributes().contains("name") &&
                    symbolTable.getParameter(method.toString(), id1.get("name")) != null)
                type1 = symbolTable.getParameter(method.toString(), id1.get("name")).getType().getName();

        String type1ExtendsImports = checkExtendsImport(type1);

        if (type1.equals("<Inherited>"))
            type1 = type0;

        if ((type0.equals("intArray") && type1.equals("int")) || type0.equals(type1)) {
            if (id0.getKind().equals("ID"))
                variables.add(id0.get("name"));
            else
                variables.add(id0.getChildren().get(0).get("name"));
        } else if (type1ExtendsImports == null ||
                (type1ExtendsImports.equals("import") && type0.equals(symbolTable.getClassName()))) {
            addSemanticErrorReport(reports, id1, "Type mismatch in operations. '" + type0 + "' to '" + type1 + "'");
            return "<Invalid>";
        }

        return type1;
    }

    private String visitID(JmmNode node, List<Report> reports){
        JmmMethod method = symbolTable.getParentMethodName(node);

        Optional<JmmNode> ancestor = node.getAncestor("MainMethod").isPresent() ?
                node.getAncestor("MainMethod") : node.getAncestor("MethodBody");

        boolean isArg = false;

        if (node.getJmmParent().getKind().equals("ReturnExpression")) {
            isArgument(node.getJmmParent().getJmmParent().getJmmChild(1), node, isArg);
        }

        Optional<JmmNode> argAncestor = node.getAncestor("MainMethod").isPresent() ?
                node.getAncestor("MainMethodArguments") : node.getAncestor("InstanceMethodArguments");
        argAncestor.ifPresent(jmmNode -> isArgument(jmmNode, node, isArg));

        if (method != null && node.getAttributes().contains("name")) {

            if (symbolTable.getLocalVar(method.toString(), node.get("name")) == null
                    && !checkImports(node.get("name"))
                    && !isArg
                    && symbolTable.getFieldByName(node.get("name")) == null
                    && symbolTable.getParameter(method.toString(), node.get("name")) == null) {
                addSemanticErrorReport(reports, node,
                        "Variable \"" + node.get("name") + "\" is not declared.");
                return "<Invalid>";
            }
        }

        if (ancestor.isEmpty()) return "";

        if (method == null) {
            if (checkImports(node.get("name"))) return "";
            addSemanticErrorReport(reports, node, "Variable \"" + node.get("name") + "\" is undefined.");
            return "<Invalid>";
        }

        Optional<String> name = node.getOptional("name");

        if (name.isEmpty()) return node.get("type");

        Symbol s = symbolTable.getLocalVar(method.toString(), name.get());

        if (!isVariableInitialized(node, reports, method, s)) return "<Invalid>";

        if (s != null)
            return s.getType().isArray() ? s.getType().getName() + "ArrayExpression" : s.getType().getName();
        else if (checkExtendsImport(name.get()) != null)
            return name.get();

        if(node.getAttributes().contains("name") &&
                symbolTable.getParameter(method.toString(), node.get("name")) != null){
            return getNodeType(node).getName();
        }

        return "<Invalid>";
    }


    /**
     * Operation Visitors
     */
    private String visitBinOp(JmmNode node, List<Report> reports){

        JmmNode lhs = node.getJmmChild(0);
        JmmNode rhs = node.getJmmChild(1);

        Type lhsType = getNodeType(lhs);
        Type rhsType = getNodeType(rhs);

        String lhsTypeName, rhsTypeName;
        boolean lhsIsArray = false, rhsIsArray = false;

        if (lhsType != null){
            lhsTypeName = lhsType.getName();
            lhsIsArray = lhsType.isArray();
        } else lhsTypeName = visit(lhs, reports);
        if (rhsType != null){
            rhsTypeName = rhsType.getName();
            rhsIsArray = rhsType.isArray();
        } else rhsTypeName = visit(rhs, reports);


        if (!lhsTypeName.equals(rhsTypeName) || rhsIsArray || lhsIsArray) {
            addSemanticErrorReport(reports, rhs,
                    "Bin OP: Type mismatch in operation. <" + lhsType + "> to <" + rhsType + ">");
            return "<Invalid>";
        }
        List<String> intOp = new ArrayList<>(Arrays.asList("Mult", "Div", "Sub", "Add", "Less"));

        if (intOp.contains(node.get("op"))){
            if ((!lhsTypeName.equals("int")
                    && !lhsTypeName.equals("import")
                    && !lhsTypeName.equals("extends"))) {
                    addSemanticErrorReport(reports, rhs,
                            "BinOP: Operations must be boolean or integers. Used '" + lhsTypeName + "' and '" + rhsTypeName + "'");
                    return "<Invalid>";
            }
        } else if (node.get("op").equals("And") || node.get("op").equals("Or")){
            if ((!lhsTypeName.equals("boolean")
                    && !lhsTypeName.equals("import")
                    && !lhsTypeName.equals("extends"))) {
                addSemanticErrorReport(reports, rhs,
                        "BinOP: Operations must be boolean or integers. Used '" + lhsTypeName + "' and '" + rhsTypeName + "'");
                return "<Invalid>";
            }
        }

        if (node.get("op").equals("Less") || node.get("op").equals("And") || node.get("op").equals("Or")) return "boolean";
        return "int";

    }

    private String visitUnaryOp(JmmNode node, List<Report> reports){
        String expressionType = visit(node.getChildren().get(0), reports);

        if (!expressionType.equals("boolean"))
            addSemanticErrorReport(reports, node,
                    "Not operator can only be applied to boolean expressions.");

        return "boolean";
    }

    /**
    * Method Visitors
    */
    private String visitMethodHeader(JmmNode node, List<Report> reports) {
        JmmNode identifier = node.getChildren().get(0);
        JmmNode method = node.getChildren().get(1);
        visit(identifier, reports);
        return visit(method, reports);
    }
    private String visitMethodBody(JmmNode node, List<Report> reports){
        variables.clear();
        return defaultVisit(node, reports);
    }

    private String visitMemberArgs(JmmNode node, List<Report> reports){
        JmmMethod ancestor = symbolTable.getParentMethodName(node);
        if(ancestor == null) return "";
        int i = 0;
        for(JmmNode child: node.getChildren()){
            String methodName = node.getJmmParent().getJmmChild(0).get("name");
            if(child.getKind().equals("ID")){
                String symbolName = child.get("name");
                boolean isMain = ancestor.getName().equals("main");
                Symbol symbol = isMain ? symbolTable.getFieldByName(symbolName) : symbolTable.getLocalVar(ancestor.toString(), symbolName) ;
                if (symbolTable.getMethodByName(methodName) != null && symbol != null
                        && !symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName().equals(symbol.getType().getName())) {
                    addSemanticErrorReport(reports, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0,
                            Integer.parseInt(child.get("column")),
                            "Argument " + child.get("name") + " of type " +  symbol.getType() +  " is of wrong type, " +
                                    symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName() + " expected at " +  methodName + " method.");
                }
            } else if (child.getAttributes().contains("type")) {
                String childType = child.get("type");
                if (child.getAttributes().contains("value")){
                    if (child.get("value").equals("this")) {
                        childType = symbolTable.getClassName();
                    }
                }
                if (!child.getKind().equals("AccessExpression")) {
                    if (symbolTable.getMethodByName(methodName) != null
                            && i < symbolTable.getMethodByName(methodName).getParameters().size()
                            && !symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName().equals(childType)) {

                        addSemanticErrorReport(reports, child.get("line") != null ? Integer.parseInt(child.get("line")) : 0, Integer.parseInt(child.get("column")),
                                "Argument is of wrong type, " + symbolTable.getMethodByName(methodName).getParameters().get(i).getType().getName() + " expected.");
                    }
                }
            }
            visit(child, reports);
            i++;
        }

        return "";
    }



    /**
     * Expression Visitors
     */
    private String visitArrayExpression(JmmNode node, List<Report> reports){
        JmmMethod method = symbolTable.getParentMethodName(node);
        JmmNode nodeToVisit;
        String name;
        Type type;
        if (node.getJmmParent().getKind().equals("ArrayAssignment")) {
            name = node.getJmmParent().getJmmChild(0).get("name");
            nodeToVisit = node.getJmmParent().getJmmChild(0);
            String assignedType = visit(node.getJmmParent().getJmmChild(2), reports);

            Symbol s = symbolTable.getLocalVar(method.toString(), name);
            if (s!= null) {
                if (!symbolTable.getLocalVar(method.toString(), name).getType().getName().equals(assignedType)) {
                    addSemanticErrorReport(reports, node, "Array can't be assigned '" + assignedType +
                            "' variables when its type is '" + symbolTable.getLocalVar(method.toString(), name).getType().getName() + "'.");
                    return "<Invalid>";
                }
            }

        } else nodeToVisit = node.getJmmChild(0);

        type = getNodeType(nodeToVisit);

        if (type != null) {
            if (!type.isArray()) {
                addSemanticErrorReport(reports, nodeToVisit, "Variable '" + type.getName() + "' cannot be indexed.");
                return "<Invalid>";
            }
        }
        for (JmmNode child : node.getChildren()) {
            if (child.getKind().equals("Literal")) {
                String idxType = visit(child, reports);

                if (!idxType.equals("int")) {
                    addSemanticErrorReport(reports, node, "Array indexes must be of type int.");
                    return "<Invalid>";
                }
            } else if (!child.equals(node.getJmmChild(0))) {
                String idx = getNodeType(child).getName();
                if (!idx.equals("int")){
                    addSemanticErrorReport(reports, node,
                            "Array indexes must be of type ints.");
                    return "<Invalid>";
                }

            } else {
                Type childType = getNodeType(child);
                String idxType;
                if (childType == null) {
                    idxType = visit(child, reports);
                } else idxType = childType.getName();
                if (!idxType.equals("int")) {
                    addSemanticErrorReport(reports, node, "Array indexes must be of type int.");
                    return "<Invalid>";
                }
            }
        }
        if (type != null)
            return type.getName();
        return "";
    }

    private String visitAccessExpression(JmmNode node, List<Report> reports){

        JmmNode child0 = node.getJmmChild(0);

        if (!isThis(child0).isEmpty()) {
            if (node.getAncestor("MainMethod").isPresent()){
                addSemanticErrorReport(reports, node,
                        "Static method main can't access 'this'.");
                return "<Invalid>";
            }
            JmmNode expressionNode = node.getJmmChild(1);
            if (symbolTable.getSuper() == null){
                if (symbolTable.getMethodByName(expressionNode.getJmmChild(0).get("name")) == null) {
                    addSemanticErrorReport(reports,
                            expressionNode.getJmmChild(0).get("line") != null ?
                                    Integer.parseInt(expressionNode.getJmmChild(0).get("line")) : 0,
                            Integer.parseInt(expressionNode.getJmmChild(0).get("column")),
                            "Method " + expressionNode.getJmmChild(0).get("name") + "() isn't declared.");
                    return "<Invalid>";
                } else if (expressionNode.getJmmChild(1).getChildren().size()
                        != symbolTable.getMethodByName(expressionNode.getJmmChild(0).get("name")).getParameters().size()) {
                    addSemanticErrorReport(reports,
                            expressionNode.getJmmChild(0).get("line") != null ?
                                    Integer.parseInt(expressionNode.getJmmChild(0).get("line")) : 0,
                            Integer.parseInt(expressionNode.getJmmChild(0).get("column")),
                            "Method " + expressionNode.getJmmChild(0).get("name")
                                    + "() has the wrong number of arguments");
                    return "<Invalid>";

                }
            }
        } else if (child0.getKind().equals("ID")){
            Type childT = getNodeType(node.getJmmChild(0));
            String childType = childT == null ? visit(node.getJmmChild(0), reports) : getNodeType(node.getJmmChild(0)).getName();
            if (childType.equals("") || childType.equals("<Invalid>")) return "<Invalid>";
            JmmMethod method = symbolTable.getParentMethodName(node);

            if ((childType.equals(symbolTable.getClassName()) || childType.equals(symbolTable.getSuper())) && symbolTable.getSuper()!=null)
                if (node.getJmmChild(1).getKind().equals("CallExpression")) {
                    JmmNode calledMethod = node.getJmmChild(1).getJmmChild(0);
                    if (calledMethod.getAttributes().contains("name")
                            &&  symbolTable.getMethodByName(calledMethod.get("name")) == null)
                        return "<Inherited>";
                }
            if (checkImports(childType))
                return "<Inherited>";

            String symbolName = child0.get("name");
            Symbol symbol = symbolTable.getLocalVar(method.toString(), symbolName);
            if (symbol != null) {
                if (checkExtendsImport(childType) == null) {
                    if (symbolTable.getMethodByName(node.getJmmChild(1).getJmmChild(0).get("name")) == null) {
                        addSemanticErrorReport(reports,
                                node.getJmmChild(1).getJmmChild(0).get("line") != null ?
                                        Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("line")) : 0,
                                Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("column")),
                                "Method " + node.getJmmChild(1).getJmmChild(0).get("name")
                                        + "() isn't declared for this type.");
                        return "<Invalid>";
                    }
                }
                if (node.getAncestor("ReturnExpression").isPresent()) {
                    if (method.getReturnType().getName().equals(childType))
                        return childType;
                    addSemanticErrorReport(reports,
                            node.getJmmChild(1).getJmmChild(0).get("line") != null ?
                                    Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("line")) : 0,
                            Integer.parseInt(node.getJmmChild(1).getJmmChild(0).get("column")),
                            "Return type expected was '" +  method.getReturnType().getName() + "' and got '"
                                    + childType + "'.");
                    return "<Invalid>";
                }

            }

            if (checkExtendsImport(childType) == null
                    && !(childType.equals(symbolTable.getClassName()) && symbolTable.getMethodByName(node.getJmmChild(1).getJmmChild(0).get("name")) != null)) {

                if(node.getKind().equals("AccessExpression")) {
                    if (node.getChildren().size() == 1) {
                        return "int";
                    } else addSemanticErrorReport(reports,
                            node.getJmmChild(0).getJmmChild(1).get("line") != null ?
                                    Integer.parseInt(node.getJmmChild(0).getJmmChild(1).get("line")) : 0,
                            Integer.parseInt(node.getJmmChild(0).getJmmChild(1).get("column")),
                            "Method " + node.getJmmChild(0).getJmmChild(1).get("name")
                                    + "() isn't declared.");
                    return "<Invalid>";
                }

            }

        }

        String ret = "Method";
        for(JmmNode child: node.getChildren()) {
            ret = visit(child, reports);
        }

        return ret;
    }

    private String visitCallExpression(JmmNode node, List<Report> reports) {
        visit(node.getJmmChild(1), reports);
        JmmMethod method = symbolTable.getMethodByName(node.getJmmChild(0).get("name"));

        if (method != null) {
            return symbolTable.getMethodByName(node.getJmmChild(0).get("name")).getReturnType().getName();
        }
        return "Method";
    }

    private String visitParenthesisExpression(JmmNode node, List<Report> reports) {
        return visit(node.getJmmChild(0), reports);
    }


    /**
     * Condition Visitors
     */

    private String visitCondition(JmmNode node, List<Report> reports) {

        for (JmmNode child : node.getChildren()) {
            String type = visit(child, reports);
            if (!type.equals("boolean")) {
                addSemanticErrorReport(reports, node, "Condition is not of type 'boolean'");
                return "<Invalid>";
            }
        }
        return "";
    }

    /**
     * Validations
     */
    private boolean isVariableInitialized(JmmNode node, List<Report>  reports, JmmMethod method, Symbol s){
        if (node.getJmmParent().getKind().equals("BinOp")
                || (node.getJmmParent().getKind().equals("IDAssignment")
                && !node.getJmmParent().getJmmChild(0).equals(node))) {
            if (!node.getJmmParent().getJmmChild(0).equals(node)
                    && !variables.contains(node.get("name"))
                    && !checkMethodParameter(node, method)
                    && !symbolTable.getFields().contains(s)) {
                addSemanticErrorReport(reports, node,
                        "Variable \"" + node.get("name") + "\" has not been initialized.");
                return false;
            }

        }
        return true;
    }

    private void isArgument(JmmNode parent, JmmNode child, boolean isArg){
        for(JmmNode arg: parent.getChildren()) {
            if (arg.getKind().equals("ID"))
                if (arg.get("name").equals(child.get("name")))
                    isArg = true;
        }
    }

    private boolean checkMethodParameter(JmmNode node,  JmmMethod method){
        for (Symbol parameter : method.getParameters()){
            if (parameter.getName().equals(node.get("name")))
                return true;
        }
        return false;
    }

    private String checkExtendsImport(String nodeType){
        if (symbolTable.getImports().contains(nodeType)) return "import";
        if (symbolTable.getSuper() != null && nodeType.equals(symbolTable.getClassName())) return "extends";
        if (nodeType.equals(symbolTable.getSuper())) return "extends";
        return null;
    }

    private String isThis(JmmNode node){
        if (node.getKind().equals("Literal") && node.get("value").equals("this")) {
            return symbolTable.getSuper() == null ? symbolTable.getClassName() : symbolTable.getSuper();
        }
        return "";
    }

    private Type getNodeType(JmmNode node) {
        Type type = null;
        JmmMethod method = symbolTable.getParentMethodName(node);
        if (node.getAttributes().contains("name")) {
            String varName = node.get("name");
            if (symbolTable.getLocalVar(method.toString(), varName) != null)
                type = symbolTable.getLocalVar(method.toString(), varName).getType();
            else if (symbolTable.getFieldByName(varName) != null)
                type = symbolTable.getFieldByName(varName).getType();
            else if (symbolTable.getParameter(method.toString(), varName) != null)
                type = symbolTable.getParameter(method.toString(), varName).getType();

        }

        return type;
    }

    private boolean checkImports(String variableName){
        List<String> imports = symbolTable.getImports();
        for (String imp : imports) {
            if (imp.equals(variableName)) return true;
        }
        return false;
    }

    /**
     * Error Reports
     */
    private void addSemanticErrorReport(List<Report>  reports, int line, int col, String message){
        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                line, col,
                message
        ));
    }
    private void addSemanticErrorReport(List<Report>  reports, JmmNode node, String message){
        reports.add(new Report(
                ReportType.ERROR,
                Stage.SEMANTIC,
                Integer.parseInt(node.get("line")),
                Integer.parseInt(node.get("column")),
                message
        ));
    }


}
