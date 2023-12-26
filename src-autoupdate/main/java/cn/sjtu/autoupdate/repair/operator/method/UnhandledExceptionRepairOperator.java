package cn.sjtu.autoupdate.repair.operator.method;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.core.dom.Modifier;

import cn.sjtu.autoupdate.apidoc.data.TypeItem;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.solutionsearch.spaces.operators.AutonomousOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtReturn;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.SpoonClassNotFoundException;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;
import spoon.support.util.QualifiedNameBasedSortedSet;

public class UnhandledExceptionRepairOperator extends MethodRepairOperator{
		

	private ArrayList<TypeItem> classes;


	public UnhandledExceptionRepairOperator(ArrayList<TypeItem> newClasses) {
		this.classes = newClasses;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();
		CtMethodImpl methodbody = getMethodBody(element);
		if(methodbody !=null) {
		
			ArrayList<TypeItem> candidates = findCandidates(modificationPoint);
			for(TypeItem candidate:candidates) {
				OperatorInstance instance = createOperatorInstance(modificationPoint, methodbody, candidate);
				instances.add(instance);
			}
		}
		return instances;

	};
	
	
	private ArrayList<TypeItem> findCandidates(ModificationPoint modificationPoint) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)modificationPoint;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int mark = prob.getMessage().lastIndexOf(" ");
		String exceptionName = prob.getMessage().substring(mark+1);
		ArrayList<TypeItem> candidates = new ArrayList<TypeItem>();
		for(TypeItem type:classes) {
			if(type.name.compareTo(exceptionName)==0) {
				candidates.add(type);
			}
		}
		return candidates;
	}

	private OperatorInstance createOperatorInstance(ModificationPoint modificationPoint, CtMethodImpl oldMethodBody,
			TypeItem exception) {
		CtMethod newMethodBody = oldMethodBody.clone();
		QualifiedNameBasedSortedSet exceptions = new QualifiedNameBasedSortedSet();
		exceptions.addAll(newMethodBody.getThrownTypes());
		CtTypeReference<Object> type = MutationSupporter.factory.createTypeReference();
		type.setSimpleName(exception.name);
		CtPackageReference pack = MutationSupporter.factory.createPackageReference();
		pack.setSimpleName(exception.getPackageName());
		type.setPackage(pack);
		
		exceptions.add(type);
		newMethodBody.setThrownTypes(exceptions);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldMethodBody, newMethodBody, oldMethodBody.getShortRepresentation()+"->"+newMethodBody.getShortRepresentation());
		return operatorInstance;
	}

	@Override
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtMethod ctst = (CtMethod)operation.getOriginal();			
			CtMethod fix = (CtMethod)operation.getModified();
			QualifiedNameBasedSortedSet exceptions = new QualifiedNameBasedSortedSet();
			exceptions.addAll(ctst.getThrownTypes());
			ctst.setThrownTypes(fix.getThrownTypes());
			fix.setThrownTypes(exceptions);
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
	public boolean undoChangesInModel(OperatorInstance operation, ProgramVariant p) {
		try {
			CtMethod ctst = (CtMethod)operation.getOriginal();			
			CtMethod fix = (CtMethod)operation.getModified();
			QualifiedNameBasedSortedSet exceptions = new QualifiedNameBasedSortedSet();
			exceptions.addAll(ctst.getThrownTypes());
			ctst.setThrownTypes(fix.getThrownTypes());
			fix.setThrownTypes(exceptions);
			
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		if(id == 168) {
			return true;
		}else {
			return false;
		}
	}

}
