package cn.sjtu.autoupdate.repair.operator.method;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.apidoc.data.MethodInfo;
import cn.sjtu.autoupdate.apidoc.data.MethodItem;
import cn.sjtu.autoupdate.apidoc.data.VarInfo;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.repair.operator.TypeMatchOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.solutionsearch.spaces.operators.AutonomousOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.code.CtVariableReadImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.support.visitor.SignaturePrinter;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class StaticMethodToNonstaticRepairOperator extends MethodRepairOperator{
	public StaticMethodToNonstaticRepairOperator( ) {
		
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();		
		if(element instanceof CtInvocationImpl) {
			CtInvocationImpl oldInvoc = (CtInvocationImpl)element;
			CtExecutableReference oldExe = oldInvoc.getExecutable();
			if(oldExe.getParameters().size()==1&&oldExe.isStatic()) {
				if(oldInvoc.getArguments().size()>0) {
					Object arg = oldInvoc.getArguments().get(0);
					if(arg instanceof CtVariableReadImpl) {
						CtVariableReadImpl vr = (CtVariableReadImpl)arg;
						CtTypeReference vt = vr.getType();
						String ert = resolveExpectedReturnType(oldInvoc);
						ArrayList<CtExecutableReference> matches = findCandidates(vt, ert);
						for(CtExecutableReference method:matches) {
							OperatorInstance instance = createOperatorInstances(modificationPoint, oldInvoc, vr, method);
							instances.add(instance);
						}	
					}
				}
			}
		}
		return instances;
	};
	
	

	private ArrayList<CtExecutableReference> findCandidates(CtTypeReference type, String expectedReturnType) {
		ArrayList<CtExecutableReference> candidates = new ArrayList<CtExecutableReference>();
		for(CtExecutableReference exe:type.getAllExecutables()) {
			if(exe.getParameters().size()==0) {
				CtTypeReference returnType = exe.getType();
				if(returnType!=null&&expectedReturnType!=null&&returnType.getSimpleName().compareTo(expectedReturnType)==0) {
					candidates.add(exe);
				}	
			}
		}
		return candidates;
	}


	
	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtInvocationImpl oldInvoc, CtVariableReadImpl variable,
			CtExecutableReference executable) {
		CtInvocation<Object> newInvoc = MutationSupporter.factory.createInvocation();
		newInvoc.setExecutable(executable);
		newInvoc.setTarget(variable);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldInvoc, newInvoc, oldInvoc.getShortRepresentation()+"->"+newInvoc.getShortRepresentation());
		return operatorInstance;
	}

	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		if(id == 115) {
			return true;
		}else {
			return false;
		}
	}

}
