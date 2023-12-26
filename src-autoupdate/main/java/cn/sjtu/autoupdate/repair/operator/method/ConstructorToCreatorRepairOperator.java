package cn.sjtu.autoupdate.repair.operator.method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtConstructorCallImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.code.CtVariableReadImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

public class ConstructorToCreatorRepairOperator extends MethodRepairOperator{
	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();		
		CtConstructorCallImpl constructor = getConstructor(element);
		if(constructor!=null) {
			CtTypeReference type = constructor.getType();
			ArrayList<CtExecutableReference> matches = findCandidates(type, constructor.getArguments());
			for(CtExecutableReference method:matches) {
				OperatorInstance instance = createOperatorInstances(modificationPoint, constructor, method);
				instances.add(instance);
			}	
		}

		return instances;
	};
	
	

	


	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtConstructorCallImpl constructor,
			CtExecutableReference method) {
		CtInvocation<Object> newInvoc = MutationSupporter.factory.createInvocation();
		newInvoc.setExecutable(method);
		CtTypeAccessImpl exp = new CtTypeAccessImpl();
		exp.setAccessedType(method.getDeclaringType());
		newInvoc.setTarget(exp);
		List args = generateFeasibleArguments(method, constructor.getArguments());
		newInvoc.setArguments(args);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, constructor, newInvoc, "new "+constructor.getExecutable().getDeclaration()+"->"+method.getSimpleName());
		return operatorInstance;
	}


	private ArrayList<CtExecutableReference> findCandidates(CtTypeReference type, List list) {
		ArrayList<String> types = new ArrayList<String>();
		for(Object o:list) {
			String rt = getReturnType(o);
			if(rt!=null) {
				if(!types.contains(rt)) {
					types.add(rt);
				}
			}
		}
		ArrayList<CtExecutableReference> candidates = new ArrayList<CtExecutableReference>();
		for(CtExecutableReference exe:type.getAllExecutables()) {
			CtTypeReference returnType = exe.getType();
			if(exe.isStatic()&&returnType.getSimpleName().compareTo(type.getSimpleName())==0&&containAllTypes(types, exe.getParameters())) {
				candidates.add(exe);				
			}
		}
		Collection<CtExecutableReference<?>> es = type.getAllExecutables();
		return candidates;
	}

	private boolean containAllTypes(ArrayList<String> types, List parameters) {
		boolean bContainAll = true;
		for(Object o:parameters) {
			CtTypeReferenceImpl type = (CtTypeReferenceImpl)o;
			if(!types.contains(type.getSimpleName())){
				bContainAll = false;
				break;
			}
		}
		return bContainAll;
	}


//	@Override
//	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
//		boolean successful = false;
//		try {
//			CtConstructorCallImpl ctst = (CtConstructorCallImpl) operation.getOriginal();			
//			CtInvocationImpl fix = (CtInvocationImpl) operation.getModified();			
//			ctst.replace(fix);
//			successful = true;
//			operation.setSuccessfulyApplied((successful));
//			log.debug(" applied: " + ctst.getParent().toString());
//		} catch (Exception ex) {
//			log.error("Error applying an operation, exception: " + ex.getMessage());
//			operation.setExceptionAtApplied(ex);
//			operation.setSuccessfulyApplied(false);
//		}
//		return successful;
//	}
//
//	@Override
//	public boolean undoChangesInModel(OperatorInstance opInstance, ProgramVariant p) {
//		try {
//			CtConstructorCallImpl ctst = (CtConstructorCallImpl) opInstance.getOriginal();
//			CtInvocationImpl fix = (CtInvocationImpl) opInstance.getModified();			
//			fix.replace(ctst);
//			return true;
//		} catch (Exception e) {
//			return false;
//		}
//	}



	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		if((id == 130)&&prob.getMessage().startsWith("The constructor")) {
			return true;
		}else {
			return false;
		}
	}
}
