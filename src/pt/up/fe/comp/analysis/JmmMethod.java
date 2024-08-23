package pt.up.fe.comp.analysis;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JmmMethod {
    private final String name;
    private final Type returnType;
    private final List<Symbol> parameters;

    private List<Symbol> vars = new ArrayList<>();

    public JmmMethod(String name, Type returnType, List<Symbol> parameters) {
        this.name = name;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public Type getReturnType() {
        return returnType;
    }

    public List<Symbol> getParameters() {
        return parameters;
    }

    public Symbol addVar(Symbol symbol) {
        for (Symbol s : vars) {
            if (Objects.equals(s.getName(), symbol.getName())) {
                return s;
            }
        }

        vars.add(symbol);
        return null;
    }

    public List<Symbol> getVars() {
        return vars;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JmmMethod jmmMethod = (JmmMethod) o;
        if (name.equals(jmmMethod.name) && returnType.equals(jmmMethod.returnType)) {
            if (parameters.size() == jmmMethod.getParameters().size()) {
                for (int i = 0; i < parameters.size(); i++) {
                    if (!parameters.get(i).getType().equals(jmmMethod.getParameters().get(i).getType())) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, returnType, parameters);
    }

    @Override
    public String toString() {
        StringBuilder parameters = new StringBuilder();

        for (Symbol s : this.parameters) {
            parameters.append(s.getType().toString()).append(" ");
        }

        return "JmmMethod{" +
                "name='" + name + '\'' +
                ", returnType=" + returnType +
                ", parameters=" + parameters +
                '}';
    }
}
