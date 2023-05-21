package ca.ubc.ece.resess.slicer.dynamic.slicer4j.datadependence;

import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AliasSet;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.SlicingWorkingSet;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.*;
import org.apache.commons.lang3.tuple.Triple;
import org.hamcrest.Condition;
import soot.Local;
import soot.Value;
import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AccessPath;
import ca.ubc.ece.resess.slicer.dynamic.core.framework.FrameworkModel;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.Traversal;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisCache;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisLogger;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.Constants;
import soot.Unit;
import soot.ValueBox;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;
import soot.util.Cons;

import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import java.util.*;

public class DynamicHeapAnalysis {
    private DynamicControlFlowGraph icdg;
    private Traversal traversal;
    private SlicingWorkingSet workingSet;
    public DynamicHeapAnalysis(DynamicControlFlowGraph icdg, AnalysisCache analysisCache, SlicingWorkingSet workingSet) {
        this.icdg = icdg;
        this.traversal = new Traversal(icdg, analysisCache);
        this.workingSet = workingSet;
    }

    static HashMap<Pair<StatementInstance, String>, StatementInstance> processed = new HashMap<>();
    public StatementSet reachingDefinitions(StatementInstance si, AccessPath ap) {
        StatementInstance def = null;
        Long fieldId = si.getFieldId();
        String fieldName = ap.getField();
        String uniqueId = ap.getField() + ":" + fieldId;
        Pair<StatementInstance, String> curProcess = new Pair<>(si, uniqueId);
        AnalysisLogger.log(true, "Getting heap def of: {}", si);
        AnalysisLogger.log(true, "with ap: {}", ap);
        StatementList trav = new StatementList();
        if(processed.containsKey(curProcess)){
            def = processed.get(curProcess);
        }
        trav.add(si);

        int pos = si.getLineNo()-1;
        StatementInstance possibleIu = null;
        while (pos >= 0 && def == null) {
            possibleIu = icdg.mapNoUnits(pos);
            if (possibleIu != null && possibleIu.getMethod().getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS)) {
                pos--;
                continue;
            }
            curProcess = new Pair<>(possibleIu, uniqueId);
            if(processed.containsKey(curProcess)){
                def = processed.get(curProcess);
                break;
            }
            def = matchFieldAddress(si, possibleIu, fieldId, fieldName);
            trav.add(possibleIu);
            pos--;
        }
        if(def != null){
            for(StatementInstance stmt : trav){
                curProcess = new Pair<>(stmt, uniqueId);
                processed.put(curProcess, def);
            }
        }

        StatementSet ret = new StatementSet();
        ret.add(def);
        AnalysisLogger.log(true, "Found def: {}", def);
        return ret;
    }

    public AliasSet getAp(StatementInstance si){
        Unit u = si.getUnit();
        List<ValueBox> useBoxes = u.getUseBoxes();
        AccessPath var = new AccessPath(si.getLineNo(), AccessPath.NOT_DEFINED, si);
        AliasSet aSet = new AliasSet();
        if (u instanceof AssignStmt) {
            AssignStmt stmt = (AssignStmt) u;
            Value r = stmt.getRightOp();
            Value l = stmt.getLeftOp();
            if(l instanceof FieldRef){
                var = new AccessPath(l.toString(), l.getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                var.add(((FieldRef) l).getField().getName(), ((FieldRef) l).getField().getType(), si);
                aSet.add(var);
            }
            if (r instanceof FieldRef) {
                if (((FieldRef) r).getUseBoxes().size() == 0) {
                    var = new AccessPath(((FieldRef) r).getField().getDeclaringClass().getName(), ((FieldRef) r).getField().getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                    var.add(((FieldRef) r).getField().getName(), ((FieldRef) r).getField().getType(), si);
                    var.setStaticField();
                    aSet.add(var);
                } else {
                    for (ValueBox v: ((FieldRef) r).getUseBoxes()){
                        var = new AccessPath(v.getValue().toString(), v.getValue().getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                        var.add(((FieldRef) r).getField().getName(), ((FieldRef) r).getField().getType(), si);
                        aSet.add(var);
                    }
                }
            } else {
                if (((Stmt) u).containsInvokeExpr()) {
                    for(ValueBox vb:useBoxes) {
                        if(vb.getValue() instanceof Local) {
                            var = new AccessPath(vb.getValue().toString(), vb.getValue().getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                            aSet.add(var);
                        }
                    }
                } else {
                    if (r instanceof Local) {
                        var = new AccessPath(r.toString(), r.getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                        aSet.add(var);
                    } else {
                        for(ValueBox vb:r.getUseBoxes()) {
                            if(vb.getValue() instanceof Local) {
                                var = new AccessPath(vb.getValue().toString(), vb.getValue().getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                                aSet.add(var);
                            }
                        }
                    }
                }
            }
        } else {
            for(ValueBox vb:useBoxes) {
                if(vb.getValue() instanceof Local) {
                    var = new AccessPath(vb.getValue().toString(), vb.getValue().getType(), si.getLineNo(), AccessPath.NOT_DEFINED, si);
                    aSet.add(var);
                }
            }
        }
        return aSet;
    }

    static HashMap<Pair<Long, String>, StatementList> foundDefs = new HashMap<>();
    public void reachingDefinitionsPrecomp() {
        AnalysisLogger.log(Constants.DEBUG, "Performing precomp");
        int pos = (int) 0;
        StatementInstance possibleIu = null;
        while (pos <= icdg.getLastLine()) {
            possibleIu = icdg.mapNoUnits(pos);
            //AnalysisLogger.log(Constants.DEBUG, "Precomp for {}", possibleIu);
            AliasSet usedVars = getAp(possibleIu);
            if (possibleIu == null || possibleIu.getMethod().getDeclaringClass().getName().startsWith(Constants.ANDROID_LIBS) || usedVars == null) {
                pos++;
                continue;
            }
            //AnalysisLogger.log(Constants.DEBUG, "# used vars = {}", usedVars.size());
            for(AccessPath usedVar : usedVars){
                Long fieldId = possibleIu.getFieldId();
                String fieldName = usedVar.getField();
                if(fieldId == -1 || fieldName.isEmpty()){
                    continue;
                }
                Pair<Long, String> cur = new Pair<>(fieldId, fieldName);
                //AnalysisLogger.log(Constants.DEBUG, "field pair = {}", cur);
                StatementList defs = foundDefs.getOrDefault(cur, new StatementList());
                StatementInstance def = matchFieldAddress(possibleIu, possibleIu, fieldId, fieldName);
                if(def != null){
                    //AnalysisLogger.log(Constants.DEBUG, "found def = {}", def);
                    defs.add(def);
                }
                foundDefs.put(cur, defs);
            }
            pos++;
        }
    }

    public StatementSet reachingDefinitionsNew(StatementInstance si, AccessPath ap) {
        StatementInstance def = null;
        Long fieldId = si.getFieldId();
        String fieldName = ap.getField();
        StatementSet ret = new StatementSet();
        if(fieldId == -1 || fieldName.isEmpty()){
            return ret;
        }
        Pair<Long, String> curProcess = new Pair<>(fieldId, fieldName);
        AnalysisLogger.log(Constants.DEBUG, "Getting heap def of: {}", si);
        AnalysisLogger.log(Constants.DEBUG, "with ap: {}", ap);
        if(!foundDefs.containsKey(curProcess)){
            reachingDefinitionsPrecomp();
        }

        def = foundDefs.get(curProcess).getClosestStatement(si);
        AnalysisLogger.log(Constants.DEBUG, "Found def: {}", def);
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

    HashSet<Pair<StatementInstance, String>> processedForwards = new HashSet<>();
    private StatementInstance matchReferenceVaraibleDefintion(StatementInstance si, StatementInstance possibleIu,
            String fieldName, StatementInstance def, Value left, Value right) {
        String usedField = ((FieldRef) right).getField().getName();
        //StatementMap chunk = traversal.getForwardChunk(possibleIu.getLineNo());
        LazyStatementMap chunk = traversal.getForwardLazyChunk(possibleIu.getLineNo());
        if(!usedField.equals(fieldName)){
            return null;
        }
        for (StatementInstance prev: chunk) {
            if (prev.getLineNo() <= possibleIu.getLineNo()) {
                continue;
            }
            Pair<StatementInstance, String> curProcess = new Pair<>(prev, fieldName);
            if(processedForwards.contains(curProcess)){
                return null;
            }
            def = getFrameworkDef(si, left, prev);
            processedForwards.add(curProcess);
            if(def != null){
                break;
            }
        }
        return def;
    }

    private StatementInstance getFrameworkDef(StatementInstance si, Value left, StatementInstance prev) {
        Stmt prevStmt = (Stmt) prev.getUnit();
        if (prevStmt.containsInvokeExpr() && traversal.isFrameworkMethod(prev)) {
            InvokeExpr expr = prevStmt.getInvokeExpr();
            if (expr instanceof InstanceInvokeExpr) {
                AccessPath instance = new AccessPath(left.toString(), left.getType(), si.getLineNo(), si.getLineNo(), si);
                if (FrameworkModel.definesInstance(prev, instance)) {
                    return prev;
                }
            }
        }
        return null;
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
