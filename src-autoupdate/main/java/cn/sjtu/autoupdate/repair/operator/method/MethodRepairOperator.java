package cn.sjtu.autoupdate.repair.operator.method;

import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.apidoc.data.MethodItem;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.repair.operator.TypeMatchOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtConstructorCallImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;

public abstract class MethodRepairOperator extends TypeMatchOperator {
	@Override
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtElement ctst = operation.getOriginal();			
			CtElement fix = operation.getModified();			
			ctst.replace(fix);
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
			fix.replace(ctst);
//			String newline = ctst.getParent().toString();
//			if(oldline.compareTo(newline)==0) {
//				System.err.println("Fail to update in "+this);
//			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	@Override
	public boolean updateProgramVariant(OperatorInstance opInstance, ProgramVariant p) {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		if(id == 100||id==130||id==115) {
			return true;
		}else {
			return false;
		}
	}
	
	protected OperatorInstance createOperatorInstance(ModificationPoint modificationPoint,
			CtExecutableReferenceImpl oldExe, MethodItem method) {
		int mark = method.fullname.lastIndexOf("(");
		String typeName = method.fullname.substring(0, mark);
		mark = typeName.lastIndexOf(".");
		String simpleName = typeName.substring(mark+1);
		typeName = typeName.substring(0, mark);
		mark = typeName.lastIndexOf(".");
		
		CtInvocation<Object> newInvoc = MutationSupporter.factory.createInvocation();
		
		CtExecutableReference<Object> executable = MutationSupporter.factory.createExecutableReference();
		newInvoc.setExecutable(executable);
		executable.setSimpleName(simpleName);
		CtTypeReference<Object> type = MutationSupporter.factory.createTypeReference();
		executable.setType(type);
		mark = typeName.lastIndexOf(".");
		type.setSimpleName(typeName.substring(mark+1));
		CtPackageReference pack = MutationSupporter.factory.createPackageReference();
		pack.setSimpleName(typeName.substring(0, mark));
		type.setPackage(pack);
		
		if(method.isStatic) {
			CtTypeAccessImpl exp = new CtTypeAccessImpl();
			exp.setAccessedType(type);
			newInvoc.setTarget(exp);
		}
		CtInvocationImpl oldInvoc = (CtInvocationImpl)oldExe.getParent();
		newInvoc.setArguments(oldInvoc.getArguments());
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldExe, newInvoc, oldExe.getShortRepresentation()+"->"+newInvoc.getShortRepresentation());
		return operatorInstance;
	}
	
	protected OperatorInstance createOperatorInstance(ModificationPoint modificationPoint, CtInvocationImpl oldInvoc,
			MethodItem method) {
		int mark = method.fullname.lastIndexOf("(");
		String typeName = method.fullname.substring(0, mark);
		mark = typeName.lastIndexOf(".");
		String simpleName = typeName.substring(mark+1);
		typeName = typeName.substring(0, mark);
		mark = typeName.lastIndexOf(".");
		
		CtInvocation<Object> newInvoc = MutationSupporter.factory.createInvocation();
		
		CtExecutableReference<Object> executable = MutationSupporter.factory.createExecutableReference();
		newInvoc.setExecutable(executable);
		executable.setSimpleName(simpleName);
		CtTypeReference<Object> type = MutationSupporter.factory.createTypeReference();
		executable.setType(type);
		mark = typeName.lastIndexOf(".");
		type.setSimpleName(typeName.substring(mark+1));
		CtPackageReference pack = MutationSupporter.factory.createPackageReference();
		pack.setSimpleName(typeName.substring(0, mark));
		type.setPackage(pack);
		
		if(method.isStatic) {
			CtTypeAccessImpl exp = new CtTypeAccessImpl();
			exp.setAccessedType(type);
			newInvoc.setTarget(exp);
		}else {
			newInvoc.setTarget(oldInvoc.getTarget());
		}
		newInvoc.setArguments(oldInvoc.getArguments());
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldInvoc, newInvoc, oldInvoc.getShortRepresentation()+"->"+newInvoc.getShortRepresentation());
		return operatorInstance;
	}
	
	protected OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtInvocationImpl oldInvoc,
			String newName) {
		int mark = newName.lastIndexOf(".");
		String simpleName = newName.substring(mark+1);
		String typeName = newName.substring(0, mark);
		CtInvocation<Object> newInvoc = MutationSupporter.factory.createInvocation();
		newInvoc.setTarget(oldInvoc.getTarget());
		CtExecutableReference<Object> executable = MutationSupporter.factory.createExecutableReference();
		newInvoc.setExecutable(executable);
		executable.setSimpleName(simpleName);
		CtTypeReference<Object> type = MutationSupporter.factory.createTypeReference();
		executable.setType(type);
		mark = typeName.lastIndexOf(".");
		type.setSimpleName(typeName.substring(mark+1));
		CtPackageReference pack = MutationSupporter.factory.createPackageReference();
		pack.setSimpleName(typeName.substring(0, mark));
		type.setPackage(pack);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldInvoc, newInvoc, oldInvoc.getShortRepresentation()+"->"+newInvoc.getShortRepresentation());
		return operatorInstance;
	}
	


}
