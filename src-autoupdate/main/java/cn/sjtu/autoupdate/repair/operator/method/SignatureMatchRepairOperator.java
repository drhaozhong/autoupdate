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
import spoon.support.reflect.declaration.CtExecutableImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.support.visitor.SignaturePrinter;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class SignatureMatchRepairOperator extends MethodRepairOperator{
	private ArrayList<MethodItem> newMethods;
	private int topN;
	

	public SignatureMatchRepairOperator(ArrayList<MethodItem> newMethods, int i ) {
		this.newMethods = newMethods;
		this.topN = i;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();
		if(element instanceof CtInvocationImpl) {
			CtInvocationImpl oldInvoc = (CtInvocationImpl)element;
			CtExecutableReference oldExe = oldInvoc.getExecutable();			
			String type = oldInvoc.getTarget().getType().getQualifiedName();
			String ert = resolveExpectedReturnType(oldInvoc);
			ArrayList<MethodItem> matches = findCandidates(oldExe, type, ert);
			for(MethodItem method:matches) {
				OperatorInstance instance = createOperatorInstance(modificationPoint, oldInvoc, method);
				instances.add(instance);
			}
		}
		return instances;
	};
	
	

	private ArrayList<MethodItem> findCandidates(CtExecutableReference oldExe, String type, String ert) {
		ArrayList<MethodItem> candidates = new ArrayList<MethodItem>();		
		for(MethodItem m:newMethods) {
//			if(m.fullname.startsWith(type)) {
//				System.out.println("Here");
//			}
			if(type!=null) {
				if(isCandidate(oldExe, ert, m)&&m.fullname.startsWith(type)) {
					m.sim = stringComparator.getUnNormalisedSimilarity(m.getShortName(), oldExe.getSimpleName());
					candidates.add(m);
				}
			}else {
				if(isCandidate(oldExe, ert, m)) {
					m.sim = stringComparator.getUnNormalisedSimilarity(m.getShortName(), oldExe.getSimpleName());
					candidates.add(m);
				}
			}
		}
		
		if(candidates.size()<topN) {
			return candidates;
		}
		
		candidates.sort(new Comparator<MethodItem>() {
            @Override
            public int compare(MethodItem m1, MethodItem m2) {
            	int value = (int)((m1.sim - m2.sim)*1000);
                return value;
            }
        });
		
		ArrayList<MethodItem> cs = new ArrayList<MethodItem>();
		for(int i=0; i<topN; i++) {
			cs.add(candidates.get(i));
		}
		return cs;
	}

	private boolean isCandidate(CtExecutableReference oldExe, String ert, MethodItem m) {
		boolean bAllMatch = false;
		if(m.paras.size()==oldExe.getParameters().size()) {
			bAllMatch = true;
			if(ert!=null) {
				if(m.returnvalue.type.indexOf(ert)<0) {
					bAllMatch = false;					
				}
			}
			for(int i=0; i<oldExe.getParameters().size(); i++) {
				String oldType = oldExe.getParameters().get(i).toString();
				String newType = m.paras.get(i).type;
				
				if(!iscompatible(newType, oldType)) {
					bAllMatch = false;
					break;
				}
			}
		}
		return bAllMatch;
	}
}
