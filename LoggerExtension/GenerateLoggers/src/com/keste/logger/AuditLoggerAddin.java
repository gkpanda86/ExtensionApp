package com.keste.logger;

import oracle.jdeveloper.audit.AbstractAuditAddin;

final class AuditLoggerAddin extends AbstractAuditAddin {
    private static final Class[] ANALYZERS = new Class[] { MethodsAnalyzer.class };

    public Class[] getAnalyzers() {
        return ANALYZERS;
    }
}
