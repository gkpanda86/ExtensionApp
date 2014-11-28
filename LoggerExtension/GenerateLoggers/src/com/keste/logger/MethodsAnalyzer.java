package com.keste.logger;

import oracle.javatools.parser.java.v2.internal.symbol.stmt.BlockStmt;
import oracle.javatools.parser.java.v2.model.SourceBlock;
import oracle.javatools.parser.java.v2.model.SourceElement;
import oracle.javatools.parser.java.v2.model.SourceFieldDeclaration;
import oracle.javatools.parser.java.v2.model.SourceFile;
import oracle.javatools.parser.java.v2.model.SourceMethod;
import oracle.javatools.parser.java.v2.model.statement.SourceBlockStatement;
import oracle.javatools.parser.java.v2.model.statement.SourceCatchClause;
import oracle.javatools.parser.java.v2.model.statement.SourceStatement;

import oracle.jdeveloper.audit.analyzer.Analyzer;
import oracle.jdeveloper.audit.analyzer.AuditContext;
import oracle.jdeveloper.audit.analyzer.Category;
import oracle.jdeveloper.audit.analyzer.Rule;
import oracle.jdeveloper.audit.analyzer.Severity;
import oracle.jdeveloper.audit.analyzer.ViolationReport;
import oracle.jdeveloper.audit.service.Localizer;

public class MethodsAnalyzer extends Analyzer {
    public MethodsAnalyzer() {
        super();
    }
    private static final Localizer LOCALIZER =
        Localizer.instance("com.keste.logger.Res");
    private final Category SAMPLE_CATEGORY =
        new Category("sample-category", LOCALIZER);

    private static final String FIX_METHOD_NAME = "fix-method";

    private final ApplyLoggerStatements fixMethodName =
        new ApplyLoggerStatements(FIX_METHOD_NAME, LOCALIZER);

    private final Rule NAME_VERIFICATION =
        new Rule("name-verification", SAMPLE_CATEGORY, Severity.WARNING,
                 LOCALIZER, fixMethodName);

    {
        // Do this to make the rule enabled by default.
        NAME_VERIFICATION.setEnabled(true);
    }

    public Rule[] getRules() {
        return new Rule[] { NAME_VERIFICATION };
    }

    public void enter(AuditContext ctx, SourceMethod method) {
        if (!NAME_VERIFICATION.isEnabled()) return;
        if(method.isConstructor()){
            return;
        }

        String name = method.getName();
        
        if (!containsLogger(method)) {
            ViolationReport vr = ctx.report(NAME_VERIFICATION);
            vr.addParameter("name", name);
        }
    }
    
    public void enter(AuditContext ctx, SourceCatchClause sc) {
      
        if (!NAME_VERIFICATION.isEnabled()) 
             return;
        
        if (!containsLogger(sc)) {
            ViolationReport vr = ctx.report(NAME_VERIFICATION);
            vr.addParameter("name", "Catch Block");
        }
    }
    
    private boolean containsLogger(SourceMethod method){
        boolean hasLogger = false;
        boolean fileHasLoggerDeclared = false;
        String logVariable = "";
        for(Object o : method.getParent().getChildren()){
            if(o instanceof SourceFieldDeclaration){
                if(((SourceFieldDeclaration)o).getSourceType().getName().equals("ADFLogger")){
                    fileHasLoggerDeclared = true;
                    logVariable = ((SourceElement)((SourceFieldDeclaration)o).getChildren().get(1)).getText();
                    logVariable = logVariable.split(" ")[0];
                }
            }
        }
        if(fileHasLoggerDeclared){
            SourceBlock block = method.getBlock();
            SourceElement elem = (SourceElement)block.getChildren().get(0);
            if(elem instanceof SourceStatement ){
                if(((SourceStatement)elem).getText().contains(logVariable)){
                    hasLogger = true;
                }
            }
        }
        return hasLogger;
    }
    
    private boolean containsLogger(SourceCatchClause sc){
        SourceElement elem = sc.getParent();
        while(!(elem instanceof SourceMethod)){
            elem = elem.getParent();
        }
        SourceMethod method = (SourceMethod)elem;
        boolean hasLogger = false;
        boolean fileHasLoggerDeclared = false;
        String logVariable = "";
        for(Object o : method.getParent().getChildren()){
            if(o instanceof SourceFieldDeclaration){
                if(((SourceFieldDeclaration)o).getSourceType().getName().equals("ADFLogger")){
                    fileHasLoggerDeclared = true;
                    logVariable = ((SourceElement)((SourceFieldDeclaration)o).getChildren().get(1)).getText();
                    logVariable = logVariable.split(" ")[0];
                }
            }
        }
        if(fileHasLoggerDeclared){
            SourceElement[] elems = sc.getContainedElements();
            for(SourceElement element : elems){
                
                if(element instanceof BlockStmt){
                    for(Object o : ((SourceBlockStatement)element).getBlock().getChildren()){
                        if(o instanceof SourceStatement ){
                            if((elem).getText().contains(logVariable)){
                                hasLogger = true;
                            }
                        }
                    }
                    //((SourceBlockStatement)element).getBlock().getChildren().add(0, startStmt);
                }
            }
            
        }
        return hasLogger;
    }
}
