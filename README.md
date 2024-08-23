

# Compilers Project

## **GROUP: 1C**

| NAME              |      NR       |  GRADE | CONTRIBUTION |
|-------------------|:-------------:|:------:|:------------:|
| Ana Luísa Marques | 201907565     | 18     | 25%          |
| José Costa        | 201907216     | 18     | 25%          |
| Margarida Raposo  | 201906784     | 18     | 25%          |
| Maria Carneiro    | 201907726     | 18     | 25%          |


GLOBAL Grade of the project: 18


## **SUMMARY**:
This compiler can generate an AST from a .jmm file after detecting syntactic and semantic errors within it. It cal also generate an .ollir file from an AST and generate a .jasmin file from OLLIR code.


## **SEMANTIC ANALYSIS**:
**Type Verification**
- Variables must be initialized;
- Variables must be defined before its usages;
- Assignments must be between elements of the same type;
- Array access must only be indexed by int values;
- An array must only be initialized by int values;
- Operations must be between elements of the same type;
- Operations are not allowed between arrays;
- Arithmetic operations must be between two int (or functions with int return type);
- Conditional operations must be between two booleans (or functions with boolean return type);

**Method Verification**
- Methods must only be invoked by objects that exist and contain the method
- Methods must belong to the current class or its superclass, or being imported (assume that the method is from the superclass when it does not exist in the current class)
- Methods must be invoked with the correct signature;
- The parameter types must match the method signature;
- Methods can be declared before or after other fuction calls it;
- The invocation of methods that are not from the current class assumes that the method exists and assume the expected types;


## **CODE GENERATION**:
Firstly, the source code is read and is transformed into an AST (all entities in the source code are being represented in the AST).The information in the AST is used to create the Symbol Table and perform the Semantic Analysis.  

The OLLIR code is created by visiting the AST and is inspired by three-address:
* Generates class header
* Generates method headers
* Translates each variable to the OLLIR syntax, applying information found in the generated SymbolTable.
* Translates each operation to OLLIR syntax
* Generates while loops and if/else conditionals
* Generates array accesses and method calls
* As needed, builds temporary variables to hold the code information

The OLLIR output is used as input to the backend stage of the compiler, responsible for the selection of JVM instructions, the assignment of the local variables of methods to the local variables of the JVM and the generation of JVM code in the Jasmin format:
* Generates imports
* Generates class headers
* Generates fields
* Generates method headers 
* Generates method instructions by:
    * Loading local variables
    * Performing operations and method invocations as needed
    * Storing variables 
    * Updating the stack limit
* Generates the stack limit and the locals limit
* Generates the return instruction

All steps of compilation are stored in their respective files.

## **PROS:**
We have compact and organized code, and have implemented most of the features specified in the project description.
Regarding the Optimizations, that we consider a positive aspect of our compiler, we used efficient JVM bytecode instructions that
allow for a better proccess of compilation, such as, for example, loading specific types like int with the appropriate instructions.

## **CONS:**
We might have some corner cases that affect the robustness of our compiler that we wish we could have explored and perfected further.
Besides the optimization we mentioned above, we did no other meaningful optimizations that could have positively affected our global compiler performance.
