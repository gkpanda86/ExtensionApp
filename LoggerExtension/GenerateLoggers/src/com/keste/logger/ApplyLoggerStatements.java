package com.keste.logger;

import java.util.ArrayList;
import java.util.List;

import oracle.javatools.parser.java.v2.JavaConstants;
import oracle.javatools.parser.java.v2.SourceFactory;
import oracle.javatools.parser.java.v2.internal.symbol.stmt.BlockStmt;
import oracle.javatools.parser.java.v2.model.SourceBlock;
import oracle.javatools.parser.java.v2.model.SourceElement;
import oracle.javatools.parser.java.v2.model.SourceFieldDeclaration;
import oracle.javatools.parser.java.v2.model.SourceFile;
import oracle.javatools.parser.java.v2.model.SourceImport;
import oracle.javatools.parser.java.v2.model.SourceLocalVariable;
import oracle.javatools.parser.java.v2.model.SourceLocalVariableDeclaration;
import oracle.javatools.parser.java.v2.model.SourceMethod;
import oracle.javatools.parser.java.v2.model.statement.SourceBlockStatement;
import oracle.javatools.parser.java.v2.model.statement.SourceCatchClause;
import oracle.javatools.parser.java.v2.model.statement.SourceReturnStatement;
import oracle.javatools.parser.java.v2.model.statement.SourceStatement;

import oracle.jdeveloper.audit.java.JavaTransformAdapter;
import oracle.jdeveloper.audit.java.JavaTransformContext;
import oracle.jdeveloper.audit.service.Localizer;
import oracle.jdeveloper.audit.transform.Transform;
import oracle.jdeveloper.audit.transform.TransformAdapter;
import oracle.jdeveloper.refactoring.RefactoringManager;

public class ApplyLoggerStatements extends Transform {
    
    public ApplyLoggerStatements(String name, Localizer localizer) {
        super(new JavaTransformAdapter(), name, localizer);
    }
    
    public void apply(JavaTransformContext context, SourceMethod method) {
        
        //Check if the ADFLogger field is declared in the class and if yes, fetch the variable name
        String logVariable = checkIfLoggerDeclaredInClass(method.getParent().getChildren());
        
        //If the ADFLogger field is not declared, then add the FieldDeclaration and import statement to the class
        if(logVariable.isEmpty()){
            logVariable = applyLoggerDeclarationInClass(method);                
        }
        
        // At this stage, we are sure that the ADFLogger field declaration is present. 
        // Apply the logger statements to the start and end of the method
        applyLoggerStatementsToMethod(method, logVariable);
      
       
        

    }
    
    public void apply(JavaTransformContext context, SourceCatchClause sc) {
        
        SourceElement elem = sc.getParent();
        while(!(elem instanceof SourceMethod)){
            elem = elem.getParent();
        }
        SourceMethod method = (SourceMethod)elem;
        
        //Check if the ADFLogger field is declared in the class and if yes, fetch the variable name
        String logVariable = checkIfLoggerDeclaredInClass(method.getParent().getChildren());
        
        //If the ADFLogger field is not declared, then add the FieldDeclaration and import statement to the class
        if(logVariable.isEmpty()){
            logVariable = applyLoggerDeclarationInClass(method);                
        }
        
        // At this stage, we are sure that the ADFLogger field declaration is present. 
        // Apply the logger statements to the start and end of the method
        applyLoggerStatementsToCatchBlock(sc, method, logVariable);
      
       
        

    }
    
    private String checkIfLoggerDeclaredInClass(List allSiblings){
        String logVariable = "";
        for(Object o : allSiblings){
            System.out.println("*** "+o.getClass());
            if(o instanceof SourceFieldDeclaration){
                if(((SourceFieldDeclaration)o).getSourceType().getName().equals("ADFLogger")){
                    logVariable = ((SourceElement)((SourceFieldDeclaration)o).getChildren().get(1)).getText();
                    logVariable = logVariable.split(" ")[0];
                }
            }
        }
        return logVariable;
    }
    
    private String applyLoggerDeclarationInClass(SourceMethod method){
        SourceFactory factory = method.getOwningSourceFile().getFactory();
        SourceImport importString = factory.createImportDeclaration("oracle.adf.share.logging.ADFLogger");
        importString.addSelf(method.getOwningSourceFile());
        String nameOfClass = method.getOwningClass().getName() + ".class";
        SourceFieldDeclaration sfd = factory.createFieldDeclaration(factory.createType("ADFLogger"), "_logger", 
                                                                    factory.createExpression("ADFLogger.createADFLogger("+nameOfClass+")"));
        sfd.addModifiers(JavaConstants.ACC_PRIVATE);
        sfd.addModifiers(JavaConstants.ACC_STATIC);
        sfd.addModifiers(JavaConstants.ACC_FINAL);
        sfd.addSelf(method.getEnclosingClass());
        
        return "_logger";
    }
    
    private void applyLoggerStatementsToMethod(SourceMethod method, String logVariable){
        
        // Variable names to hold the start time and end time
        String startVar = "st";
        String endVar = "et";
        
        // Iterate through the children of the method block to get hold of all the names of the variables declared within it
        // This is necessary as we donot want the variable we are adding to clash with an existing variable name
        List<String> variableNames = new ArrayList<String>();
        for(Object o : method.getBlock().getChildren()){
            if(o instanceof SourceLocalVariableDeclaration){
                SourceLocalVariableDeclaration sl = (SourceLocalVariableDeclaration)o;
                for(Object o1 : sl.getVariables()){
                    if(o1 instanceof SourceLocalVariable){
                        SourceLocalVariable slv = (SourceLocalVariable)o1;
                        variableNames.add(slv.getName());
                    }
                }
            }
        }
        
        // Check that the start and end variable names we are about to declare are not already present.
        // If they are present then append a number to the end and keep on incrementing it till you find a unique variable
        int i = 1;
        while(variableNames.contains(startVar)){
            startVar += i;
            ++i;
        }
        i=1;
        while(variableNames.contains(endVar)){
            endVar += 1;
            ++i;
        }
        
        
        String methodName = method.getName();
        SourceFactory factory = method.getOwningSourceFile().getFactory();
        
        // Create Local variable declaration statement for start time
        SourceLocalVariableDeclaration slvdStart = factory.createLocalVariableDeclaration(factory.createType("long"), startVar, 
                                                                          factory.createExpression("System.currentTimeMillis()"));
        //Create local variable declaration statement for end time
        SourceLocalVariableDeclaration slvdEnd = factory.createLocalVariableDeclaration(factory.createType("long"), endVar, 
                                                                          factory.createExpression("System.currentTimeMillis()"));
        // Create logger statemnt for start of method
        SourceStatement startStmt = factory.createStatementFromText(logVariable+".info(\" Start of method "+method.getOwningClass().getName()+"."+methodName+"\");");
        //Create logger statement for end of method
        SourceStatement endStmt = factory.createStatementFromText(logVariable+".info(\" End of method "+method.getOwningClass().getName()+"."+methodName+" Total Time taken = \"+("+endVar+"-"+startVar+"));");
        //If the method has a return statement at the end, then add the variable declaration and end statemnt just prior to that
        //else add it right at the end (for return type void)
        SourceElement lastElem = (SourceElement)method.getBlock().getChildren().get(method.getBlock().getChildren().size()-1);
        if(lastElem instanceof SourceReturnStatement){
            method.getBlock().getChildren().add(method.getBlock().getChildren().size()-1, slvdEnd);
            method.getBlock().getChildren().add(method.getBlock().getChildren().size()-1, endStmt);
        }
        else{
            method.getBlock().getChildren().add(method.getBlock().getChildren().size()-1, slvdEnd);
            method.getBlock().getChildren().add(method.getBlock().getChildren().size(), endStmt);
        }
        // Add the start variable and start logger statment right at the beginning of the method
        method.getBlock().getChildren().add(0, startStmt);
        method.getBlock().getChildren().add(0, slvdStart);
    }

    private void applyLoggerStatementsToCatchBlock(SourceCatchClause sc, SourceMethod method, 
                                                   String logVariable) {
        String methodName = method.getName();
        SourceFactory factory = method.getOwningSourceFile().getFactory();
        String var = sc.getCatchVariable().getName();
        var = var + ".getMessage()";
        SourceStatement startStmt = factory.createStatementFromText(logVariable+".severe(\" Exception in catch block for method:: "+methodName+" \"+"+var+");");
        SourceElement[] elems = sc.getContainedElements();
        for(SourceElement element : elems){
            
            if(element instanceof BlockStmt){
                ((SourceBlockStatement)element).getBlock().getChildren().add(0, startStmt);
            }
        }
    }
}
