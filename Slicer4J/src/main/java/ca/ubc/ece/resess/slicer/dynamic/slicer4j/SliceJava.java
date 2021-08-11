package ca.ubc.ece.resess.slicer.dynamic.slicer4j;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import ca.ubc.ece.resess.slicer.dynamic.slicer4j.datadependence.DynamicHeapAnalysis;
import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AccessPath;
import ca.ubc.ece.resess.slicer.dynamic.core.accesspath.AliasSet;
import ca.ubc.ece.resess.slicer.dynamic.core.datadependence.BackwardStaticFieldAnalysis;
import ca.ubc.ece.resess.slicer.dynamic.core.graph.DynamicControlFlowGraph;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.DynamicSlice;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.SliceMethod;
import ca.ubc.ece.resess.slicer.dynamic.core.slicer.SlicingWorkingSet;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementInstance;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementList;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementMap;
import ca.ubc.ece.resess.slicer.dynamic.core.statements.StatementSet;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisCache;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisLogger;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.AnalysisUtils;
import ca.ubc.ece.resess.slicer.dynamic.core.utils.Constants;
import soot.SootField;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.FieldRef;
import soot.jimple.InstanceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.toolkits.scalar.Pair;

public class SliceJava extends SliceMethod{

    public SliceJava(DynamicControlFlowGraph icdg, boolean frameworkModel, boolean dataFlowsOnly, boolean controlFlowOnly, boolean sliceOnce, SlicingWorkingSet workingSet, AnalysisCache analysisCache) {
        super(icdg, frameworkModel, dataFlowsOnly, controlFlowOnly, sliceOnce, workingSet, analysisCache);
    }

    public DynamicSlice slice(List<StatementInstance> start, Set<AccessPath> variables) {
        DynamicSlice dynamicSlice = new DynamicSlice();
        for (StatementInstance si: start) {
            DynamicSlice newSlice = slice(si, variables);
            dynamicSlice = dynamicSlice.union(newSlice);
        }
        return dynamicSlice;
    }

    @Override
    public StatementSet getDataDependence(SlicingWorkingSet workingSet, Pair<StatementInstance, AccessPath> p,
            StatementInstance stmt, AccessPath var, StatementMap chunk, StatementSet def, AliasSet usedVars) {
        if (var.getField().equals("")) {
            def = localReachingDef(stmt, var, chunk, usedVars, frameworkModel);
            AnalysisLogger.log(Constants.DEBUG, "Local def {}", def);
        } else if (var.isStaticField()) {
            AnalysisLogger.log(Constants.DEBUG, "Getting static heap def of {}-{}", var, var.getClassPath());
            def = staticFieldDef(stmt, var);
            if (def.isEmpty()) {
                AnalysisLogger.log(Constants.DEBUG, "Static heap def of {} is None", var);
            } else {
                AnalysisLogger.log(Constants.DEBUG, "Static heap def of {} is {}", var, def);
            }
        } else {
            AnalysisLogger.log(Constants.DEBUG, "Getting dynamic heap def of {}", var);
            def = (new DynamicHeapAnalysis(icdg, analysisCache)).reachingDefinitions(stmt, var);
            AnalysisLogger.log(Constants.DEBUG, "Dynamic heap def of {} is {}", var, def);
        }
        if (!usedVars.isEmpty() && def != null) {
            for (StatementInstance iu: def) {
                for (AccessPath usedVar: usedVars) {
                    workingSet.add(iu, usedVar, p, "data");
                }
            }
        }
        return def;
    }

    private StatementSet staticFieldDef(StatementInstance iu, AccessPath ap) {
        AnalysisLogger.log(Constants.DEBUG, "Getting static heap def for {}", iu);
        StatementSet aliasPath = new StatementSet();
        BackwardStaticFieldAnalysis bw = new BackwardStaticFieldAnalysis(icdg, iu, ap, aliasPath);
        bw.run();
        StatementList orderedPath = new StatementList();
        AnalysisLogger.log(Constants.DEBUG, "Alias path is {}", aliasPath);
        for (StatementInstance pathElem : aliasPath) {
            if (pathElem.getLineNo() < iu.getLineNo() || ap.isStaticField()) {
                orderedPath.add(pathElem);
            }
        }
        addInstances(orderedPath);
        Collections.sort(orderedPath, (lhs, rhs) -> {
            if (rhs.getLineNo() > lhs.getLineNo()) {
                return 1;
            } else if (rhs.getLineNo() < lhs.getLineNo()) {
                return -1;
            }
            return 0;
        });
        StatementInstance defSubSig = matchField(iu, orderedPath, SliceJava::getFieldSubSignature);
        StatementSet ret = new StatementSet();
        if (defSubSig != null) {
            int index = orderedPath.indexOf(defSubSig);
            ret.addAll(orderedPath.subList(index, index+1));
        }
        return lastDefined(ret);
    }

    private void addInstances(StatementList orderedPath) {
        boolean found = false;
        for (StatementInstance si: orderedPath) {
            if (traversal.isFrameworkMethod(si) && !si.isAssign() && (AnalysisUtils.getCallerExp(si) instanceof InstanceInvokeExpr)) {
                found = matchReferenceToVariable(orderedPath, found, si);
            }
            if (found) {
                return;
            }
        }
    }
    
    private boolean matchReferenceToVariable(StatementList orderedPath, boolean found, StatementInstance si)
        throws Error {
        Value base = ((InstanceInvokeExpr) AnalysisUtils.getCallerExp(si)).getBase();
        StatementMap chunk = traversal.getChunk(si.getLineNo());
        if (chunk != null) {
            for (StatementInstance sii: chunk.values()) {
                if (sii.isAssign() && (((AssignStmt) sii.getUnit()).getLeftOp().equals(base))) {
                    orderedPath.add(sii);
                    found = true;
                    break;
                }
            }
        }
        return found;
    }

    public static String getFieldSubSignature(SootField sf) {
        return sf.getSubSignature();
    }

    private StatementInstance matchField(StatementInstance iu, StatementList orderedPath, Function<SootField, String> signatureFunc) {
        StatementInstance def = null;
        if (!(iu.getUnit() instanceof AssignStmt)) {
            return null;
        }
        SootField sf = ((FieldRef) (((AssignStmt) iu.getUnit()).getRightOp())).getField();
        for (int i = 0; i < orderedPath.size(); i++) {
            StatementInstance instr = orderedPath.get(i);
            Unit u = instr.getUnit();
            if ((u instanceof AssignStmt) && iu.isAfter(instr)) {
                if (instr.containsInvokeExpr()) {
                    def = checkAssignInvokeSignature(sf, instr, signatureFunc);
                } else {
                    def = checkAssignSignature(sf, i, instr, orderedPath, signatureFunc);
                }
            }
            if (def != null) {
                break;
            }
        }
        AnalysisLogger.log(Constants.DEBUG, "Found a def {}", def);
        return def;
    }


    private StatementInstance checkAssignInvokeSignature(SootField sf, StatementInstance instr, Function<SootField, String> signatureFunc) {
        StatementInstance matched = null;
        Value invokeExpr = ((AssignStmt) instr.getUnit()).getRightOp();
        if (invokeExpr instanceof InvokeExpr) {
            Stmt stmt = (Stmt) instr.getUnit();
            if (stmt.containsInvokeExpr()) {
                InvokeExpr expr = stmt.getInvokeExpr();
                if ((expr instanceof InstanceInvokeExpr)) {
                    String base = ((InstanceInvokeExpr) expr).getBase().toString();
                    matched = matchInstanceToField(sf, instr, signatureFunc, matched, base);
                }
            }
        }
        return matched;
    }


    private StatementInstance matchInstanceToField(SootField sf, StatementInstance instr,
            Function<SootField, String> signatureFunc, StatementInstance matched, String base) {
        StatementInstance prev = icdg.mapNoUnits(instr.getLineNo()-1);
        if (prev != null && prev.isAssign()) {
            Value right = ((AssignStmt) prev.getUnit()).getRightOp();
            if ((right instanceof FieldRef) && signatureFunc.apply(((FieldRef) right).getField()).equals(signatureFunc.apply(sf))) {
                Value left = ((AssignStmt) prev.getUnit()).getLeftOp();
                if (left.toString().equals(base)) {
                    matched = instr;
                }
            }
        }
        return matched;
    }

    private StatementInstance checkAssignSignature(SootField sf, int i, StatementInstance instr, StatementList orderedPath, Function<SootField, String> signatureFunc) {
        AssignStmt stmt = (AssignStmt) instr.getUnit();
        Value left = stmt.getLeftOp();
        Value right = stmt.getRightOp();
        StatementInstance def = null;
        if ((left instanceof FieldRef) && signatureFunc.apply(((FieldRef) left ).getField()).equals(signatureFunc.apply(sf))) {
            def = instr;
        } else if ((right instanceof FieldRef) && signatureFunc.apply(((FieldRef) right).getField()).equals(signatureFunc.apply(sf)) && i > 0) {
            String instanceStr = stmt.getLeftOp().toString();
            StatementInstance nextInstr = icdg.mapNoUnits(instr.getLineNo()+1);
            def = checkInstructionSignature(instanceStr, nextInstr);
            if (def == null) {
                nextInstr = orderedPath.get(i-1);
                def = checkInstructionSignature(instanceStr, nextInstr);
            }
        }
        return def;
    }

    private StatementInstance checkInstructionSignature(String instanceStr, StatementInstance nextInstr) {
        StatementInstance def = null;
        if (nextInstr != null) {
            Stmt nextStmt = (Stmt) nextInstr.getUnit();
            if (nextStmt.containsInvokeExpr()) {
                InvokeExpr expr = nextStmt.getInvokeExpr();
                if ((expr instanceof InstanceInvokeExpr) && ((InstanceInvokeExpr) expr).getBase().toString().equals(instanceStr)) {
                    def = nextInstr;
                }
            }
        }
        return def;
    }

    private StatementSet lastDefined(StatementSet aliasPath) {
        StatementSet lastDefined = new StatementSet();
        Set<Pair<String, Type>> definedFields = new HashSet<>();
        for (StatementInstance si: aliasPath) {
            if (si.isAssign()) {
                Value leftOp = ((AssignStmt) si.getUnit()).getLeftOp();
                Value rightOp = ((AssignStmt) si.getUnit()).getRightOp();
                if (leftOp instanceof FieldRef) {
                    SootField sf = ((FieldRef) leftOp).getField();
                    Pair<String, Type> fieldNameType = new Pair<>(sf.getName(), sf.getType());
                    if (!definedFields.contains(fieldNameType)) {
                        lastDefined.add(si);
                        definedFields.add(fieldNameType);
                    }
                } else if (rightOp instanceof InvokeExpr) {
                    lastDefined.add(si);
                }
            } else {
                lastDefined.add(si);
            }
            
        }
        return lastDefined;
    }
}