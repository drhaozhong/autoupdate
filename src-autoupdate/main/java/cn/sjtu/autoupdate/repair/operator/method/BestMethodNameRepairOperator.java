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
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.support.visitor.SignaturePrinter;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class BestMethodNameRepairOperator extends MethodRepairOperator{
	private ArrayList<MethodItem> newMethods;
	private int topN;
	private Levenshtein stringComparator = new Levenshtein();

	
	public BestMethodNameRepairOperator(ArrayList<MethodItem> newMethods, int topN) {
		this.newMethods = newMethods;
		this.topN = topN;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();		
		
		if(element instanceof CtInvocationImpl) {
			CtInvocationImpl oldInvoc = (CtInvocationImpl)element;
			String oldName = oldInvoc.getExecutable().getSimpleName();
			String typeName = null;
			if(oldInvoc.getExecutable().getDeclaringType()!=null) {
				typeName = oldInvoc.getExecutable().getDeclaringType().getQualifiedName();
			}
			
			ArrayList<MethodItem> matches = findTopCandidates(oldName, typeName, oldInvoc.getExecutable().getParameters().size());
			for(MethodItem method:matches) {
				OperatorInstance instance = createOperatorInstance(modificationPoint, oldInvoc, method);
				instances.add(instance);
			}
		}else if(element.getParent() instanceof CtExecutableReferenceImpl) {
			CtExecutableReferenceImpl oldInvoc = (CtExecutableReferenceImpl)element.getParent();
			String oldName = oldInvoc.getSimpleName();
			String typeName = null;
			if(oldInvoc.getDeclaringType()!=null) {
				typeName = oldInvoc.getDeclaringType().getQualifiedName();
			}
			
			ArrayList<MethodItem> matches = findTopCandidates(oldName, typeName, oldInvoc.getParameters().size());
			for(MethodItem method:matches) {
				OperatorInstance instance = createOperatorInstance(modificationPoint, oldInvoc, method);
				instances.add(instance);
			}
		}
	
		return instances;
	};
	
	

	

	private ArrayList<MethodItem> findTopCandidates(String methodName, String typeName, int size) {
		ArrayList<MethodItem> candidates = new ArrayList<MethodItem>();
		for(MethodItem m:newMethods) {
			if(m.paras.size()==size) {
				m.sim = stringComparator.getUnNormalisedSimilarity(m.getShortName(), methodName);
				if(typeName!=null&&m.fullname.indexOf(typeName)==0) {
					
				}else {
					candidates.add(m);
				}
			}	
		}
		candidates.sort(new Comparator<MethodItem>() {
            @Override
            public int compare(MethodItem m1, MethodItem m2) {
            	int value = (int)((m1.sim - m2.sim)*1000);
                return value;
            }
        });
		if(candidates.size()<topN) {
			return candidates;
		}
		ArrayList<MethodItem> cs = new ArrayList<MethodItem>();
		for(int i=0; i<topN; i++) {
			cs.add(candidates.get(i));
		}
		return cs;
	}
	
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		boolean bValid = false;
		if(id == 100||id==130||id==115) {
			bValid = true;
		}
		if(id == 50) {
			CtElement element = point.getCodeElement();
			CtElement parent = element.getParent();
			if(parent instanceof CtExecutableReferenceImpl) {
				bValid = true;
			}
		}
		return bValid;
	}
	
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtElement ctst = operation.getOriginal();	
			CtElement fix = operation.getModified();
			if(ctst instanceof CtExecutableReferenceImpl) {
				CtElement parent = ctst.getParent();
				parent.replace(fix);
			}else {
				ctst.replace(fix);
			}
			successful = true;
			operation.setSuccessfulyApplied((successful));
			log.debug(" applied: " + ctst.getParent().toString());
		} catch (Exception ex) {
			log.error("Error applying an operation, exception: " + ex.getMessage());
			operation.setExceptionAtApplied(ex);
			operation.setSuccessfulyApplied(false);
		}
		return successful;
	}

	@Override
	public boolean undoChangesInModel(OperatorInstance opInstance, ProgramVariant p) {
		try {
//			if(this instanceof BestMethodNameRepairOperator) {
//				System.out.println("Here");
//			}
			CtElement ctst = opInstance.getOriginal();
			CtElement fix = opInstance.getModified();	
//			String oldline = ctst.getParent().toString();
			if(ctst instanceof CtExecutableReferenceImpl) {
				CtElement parent = ctst.getParent();
				parent.replace(fix);
			}else {
				ctst.replace(fix);
			}
//			String newline = ctst.getParent().toString();
//			if(oldline.compareTo(newline)==0) {
//				System.err.println("Fail to update in "+this);
//			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
