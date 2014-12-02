package com.keste.logger;

import java.util.List;

import oracle.javatools.parser.java.v2.JavaConstants;
import oracle.javatools.parser.java.v2.SourceFactory;
import oracle.javatools.parser.java.v2.internal.symbol.stmt.BlockStmt;
import oracle.javatools.parser.java.v2.model.SourceBlock;
import oracle.javatools.parser.java.v2.model.SourceElement;
import oracle.javatools.parser.java.v2.model.SourceFieldDeclaration;
import oracle.javatools.parser.java.v2.model.SourceFile;
import oracle.javatools.parser.java.v2.model.SourceImport;
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
        String methodName = method.getName();
        SourceFactory factory = method.getOwningSourceFile().getFactory();
        SourceStatement startStmt = factory.createStatementFromText(logVariable+".info(\" Start of method "+method.getOwningClass().getName()+"."+methodName+"\");");
        SourceStatement endStmt = factory.createStatementFromText(logVariable+".info(\" End of method "+method.getOwningClass().getName()+"."+methodName+"\");");
        SourceElement lastElem = (SourceElement)method.getBlock().getChildren().get(method.getBlock().getChildren().size()-1);
        if(lastElem instanceof SourceReturnStatement){
            method.getBlock().getChildren().add(method.getBlock().getChildren().size()-1, endStmt);
        }
        else{
            method.getBlock().getChildren().add(method.getBlock().getChildren().size(), endStmt);
        }
         
        method.getBlock().getChildren().add(0, startStmt);
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
            System.out.println("Text "+element.getText());
            System.out.println("Class "+element.getClass());
            
            if(element instanceof BlockStmt){
                ((SourceBlockStatement)element).getBlock().getChildren().add(0, startStmt);
            }
        }
    }
}
