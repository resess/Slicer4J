package ca.ubc.ece.resess.slicer.dynamic.slicer4j.datadependence;

import soot.Value;
import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AccessPath;
import ca.ubc.ece.resess.slicer.dynamic.core.framework.FrameworkModel;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.Traversal;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementMap;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementSet;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisCache;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisLogger;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.Constants;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;

public class DynamicHeapAnalysis {
    private DynamicControlFlowGraph icdg;
    private Traversal traversal;
    public DynamicHeapAnalysis(DynamicControlFlowGraph icdg, AnalysisCache analysisCache) {
        this.icdg = icdg;
        this.traversal = new Traversal(icdg, analysisCache);
    }

    public StatementSet reachingDefinitions(StatementInstance si, AccessPath ap) {
        StatementInstance def = null;
        Long fieldId = si.getFieldId();
        String fieldName = ap.getField();
        int pos = si.getLineNo()-1;
        StatementInstance possibleIu = null;
        AnalysisLogger.log(true, "Getting heap def of {}", si);
        while (pos >= 0 && def == null) {
            possibleIu = icdg.mapNoUnits(pos);
            if (possibleIu != null && possibleIu.getMethod().getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS)) {
                pos--;
                continue;
            }
            def = matchFieldAddress(si, possibleIu, fieldId, fieldName);
            pos--;
        }

        StatementSet ret = new StatementSet();
        ret.add(def);
        return ret;
    }

    private StatementInstance matchFieldAddress(StatementInstance si, StatementInstance possibleIu, Long fieldId, String fieldName) {
        StatementInstance def = null;
        if (possibleIu != null && possibleIu.getFieldId() != null && possibleIu.getFieldId().equals(fieldId)) {
            Unit u = possibleIu.getUnit();
            if (u instanceof AssignStmt) {
                AssignStmt assignStmt = ((AssignStmt) u);
                Value left = assignStmt.getLeftOp();
                Value right = assignStmt.getRightOp();
                if (left instanceof FieldRef) {
                    def = matchFieldDefintion(possibleIu, fieldName, def, left);
                } else if (right instanceof FieldRef) {
                    def = matchReferenceVaraibleDefintion(si, possibleIu, fieldName, def, left, right);
                }
            }
        }
        return def;
    }

    private StatementInstance matchReferenceVaraibleDefintion(StatementInstance si, StatementInstance possibleIu,
            String fieldName, StatementInstance def, Value left, Value right) {
        String usedField = ((FieldRef) right).getField().getName();
        StatementMap chunk = traversal.getForwardChunk(possibleIu.getLineNo());
        for (StatementInstance prev: chunk.values()) {
            if (prev.getLineNo() <= possibleIu.getLineNo()) {
                continue;
            }
            if (usedField.equals(fieldName)) {
                Stmt prevStmt = (Stmt) prev.getUnit();
                if (prevStmt.containsInvokeExpr() && traversal.isFrameworkMethod(prev)) {
                    InvokeExpr expr = prevStmt.getInvokeExpr();
                    if (expr instanceof InstanceInvokeExpr) {
                        AccessPath instance = new AccessPath(left.toString(), left.getType(), si.getLineNo(), si.getLineNo(), si);
                        if (FrameworkModel.definesInstance(prev, instance)) {
                            def = prev;
                            break;
                        }
                    }
                }
            }
        }
        return def;
    }

    private StatementInstance matchFieldDefintion(StatementInstance possibleIu, String fieldName, StatementInstance def,
            Value left) {
        String definedField = ((FieldRef) left).getField().getName();
        if (definedField.equals(fieldName)) {
            def = possibleIu;
        }
        return def;
    }
}
