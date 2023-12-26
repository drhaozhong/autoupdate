package cn.sjtu.autoupdate.repair.operator.type;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.repair.operator.TypeMatchOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExecutableReferenceExpression;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtTypeAccess;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

public abstract class TypeRepairOperator extends TypeMatchOperator{
	
	protected OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, 
			CtTypeReferenceImpl oldType, String newTypeName) {
		CtTypeReference newType = createNewType(oldType, newTypeName);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldType, newType, oldType.getQualifiedName()+"->"+newType.getQualifiedName());
		return operatorInstance;
	}

	private CtTypeReference createNewType(CtTypeReference oldType, String newTypeName) {
		CtPackageReference newPack = MutationSupporter.factory.createPackageReference();
		int mark = newTypeName.lastIndexOf(".");
		String newPackName = newTypeName.substring(0, mark);
		String newShortName = newTypeName.substring(mark+1);
		newPack.setSimpleName(newPackName);
		CtTypeReference newType = oldType.clone();
		newType.setPackage(newPack);
		newType.setSimpleName(newShortName);
		return newType;
	}
	
	protected OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtInvocationImpl oldCall,
			String newTypeName) {
		CtInvocation newCall = oldCall.clone();
		CtExecutableReference exe = newCall.getExecutable();
		CtTypeReference oldType = exe.getDeclaringType();
		if(oldType!=null) {
			CtTypeReference newType = createNewType(oldType, newTypeName);
			exe.setDeclaringType(newType);
		}
		newCall.setExecutable(exe);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldCall, newCall,oldCall.getShortRepresentation()+"->"+newCall.getShortRepresentation());
		return operatorInstance;
	}
	
	protected OperatorInstance createOperatorInstances(ModificationPoint modificationPoint,
			CtConstructorCall oldCall, String newTypeName) {
		CtConstructorCall newCall = oldCall.clone();
		CtExecutableReference exe = newCall.getExecutable();
		CtTypeReference oldType = exe.getDeclaringType();
		CtTypeReference newType = createNewType(oldType, newTypeName);
		exe.setDeclaringType(newType);
		newCall.setExecutable(exe);
		newCall.setType(newType);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldCall, newCall,oldCall.getShortRepresentation()+"->"+newCall.getShortRepresentation());
		return operatorInstance;
	}
	
	protected OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtFieldReadImpl oldField,
			String newTypeName) {
		CtFieldRead newField = oldField.clone();
		CtTypeAccess target = (CtTypeAccess)newField.getTarget();
		if(target!=null) {
			CtTypeReference oldType = target.getAccessedType();
			CtTypeReference newType = createNewType(oldType, newTypeName);
			target.setAccessedType(newType);
		}else {
			CtPackageReference newPack = MutationSupporter.factory.createPackageReference();
			int mark = newTypeName.lastIndexOf(".");
			String newPackName = newTypeName.substring(0, mark);
			String newShortName = newTypeName.substring(mark+1);
			newPack.setSimpleName(newPackName);
			CtTypeReference newType = MutationSupporter.factory.createTypeReference();
			newType.setPackage(newPack);
			newType.setSimpleName(newShortName);
			newField.setType(newType);
		}
		
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldField, newField,oldField.getShortRepresentation()+"->"+newField.getShortRepresentation());
		return operatorInstance;
	}
	
	protected String getErrorCodeName(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		String msg = prob.getMessage();
		int mark = msg.indexOf(" ");
		msg = msg.substring(0, mark);	
		return msg;
	}
	
	protected CtElement getErrorCode(String errorName, CtElement element) {
		if(element instanceof CtTypeReferenceImpl) {	
			CtTypeReferenceImpl type = (CtTypeReferenceImpl)element;
			for(Object o:type.getActualTypeArguments()) {
				CtTypeReferenceImpl subType = (CtTypeReferenceImpl)o;
				if(subType.getSimpleName().compareTo(errorName)==0) {
					return subType;
				}
			}
		}
		CtElement ce = null;
		if(element.toString().indexOf(errorName)>=0) {
			ce = element;
		}
		return ce;
	}
	
	@Override
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtElement ctst = operation.getOriginal();
			String oldline = ctst.getParent().toString();
			CtElement fix = operation.getModified();		
//			if(ctst.toString().indexOf("NPOIFSFileSystem")>=0) {
//			if(ctst.toString().indexOf("MutableProperty")>=0) {
//				System.out.println("Here");
//			}
			if(ctst instanceof CtTypeReferenceImpl) {	
				CtTypeReferenceImpl oldType = (CtTypeReferenceImpl)ctst;
				CtTypeReferenceImpl newType = (CtTypeReferenceImpl)fix;
				CtPackageReference oldPack = oldType.getPackage();
				String oldName = oldType.getSimpleName();
				oldType.setPackage(newType.getPackage());
				oldType.setSimpleName(newType.getSimpleName());
				newType.setPackage(oldPack);
				newType.setSimpleName(oldName);
			}else if(ctst instanceof CtConstructorCall) {
				CtConstructorCall oldCall = (CtConstructorCall)ctst;
				CtConstructorCall newCall = (CtConstructorCall)fix;
				CtExecutableReference oldExe = oldCall.getExecutable();
				CtTypeReference oldType = oldCall.getType();
				oldCall.setExecutable(newCall.getExecutable());
				oldCall.setType(newCall.getType());
				newCall.setExecutable(oldExe);
				newCall.setType(oldType);
			}else if(ctst instanceof CtInvocationImpl) {
				CtInvocationImpl oldCall = (CtInvocationImpl)ctst;
				CtInvocationImpl newCall = (CtInvocationImpl)fix;
				CtExecutableReference oldExe = oldCall.getExecutable();
				oldCall.setExecutable(newCall.getExecutable());
				newCall.setExecutable(oldExe);
			}else if(ctst instanceof CtFieldReadImpl) {
				CtFieldReadImpl oldField = (CtFieldReadImpl)ctst;
				CtFieldReadImpl newField = (CtFieldReadImpl)fix;
				CtExpression oldTarget = oldField.getTarget();
				CtTypeReference oldName = oldField.getType();
				oldField.setTarget(newField.getTarget());
				oldField.setType(newField.getType());
				newField.setTarget(oldTarget);
				newField.setType(oldName);
			}else {
				ctst.replace(fix);
			}
			String newline = ctst.getParent().toString();
			if(oldline.compareTo(newline)==0) {
//				ctst.toString();
				System.err.println("Fail to update in "+this);
				successful = false;
			}else {
				successful = true;
			}
			operation.setSuccessfulyApplied((successful));
			log.debug(" applied: " + ctst.toString());
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
			CtElement ctst = opInstance.getOriginal();
			CtElement fix = opInstance.getModified();			
			if(ctst instanceof CtTypeReferenceImpl) {	
				CtTypeReferenceImpl oldType = (CtTypeReferenceImpl)ctst;
				CtTypeReferenceImpl newType = (CtTypeReferenceImpl)fix;
				CtPackageReference oldPack = oldType.getPackage();
				String oldName = oldType.getSimpleName();
				oldType.setPackage(newType.getPackage());
				oldType.setSimpleName(newType.getSimpleName());
				newType.setPackage(oldPack);
				newType.setSimpleName(oldName);
			}else if(ctst instanceof CtConstructorCall) {
				CtConstructorCall oldType = (CtConstructorCall)ctst;
				CtConstructorCall newType = (CtConstructorCall)fix;
				CtExecutableReference oldExe = oldType.getExecutable();
				oldType.setExecutable(newType.getExecutable());
				newType.setExecutable(oldExe);
			}else if(ctst instanceof CtInvocationImpl) {
				CtInvocationImpl oldType = (CtInvocationImpl)ctst;
				CtInvocationImpl newType = (CtInvocationImpl)fix;
				CtExecutableReference oldExe = oldType.getExecutable();
				oldType.setExecutable(newType.getExecutable());
				newType.setExecutable(oldExe);
			}else if(ctst instanceof CtFieldReadImpl) {
				CtFieldReadImpl oldType = (CtFieldReadImpl)ctst;
				CtFieldReadImpl newType = (CtFieldReadImpl)fix;
				CtExpression oldTarget = oldType.getTarget();
				CtTypeReference oldName = oldType.getType();
				oldType.setTarget(newType.getTarget());
				oldType.setType(newType.getType());
				newType.setTarget(oldTarget);
				newType.setType(oldName);
			}else {
				ctst.replace(fix);
			}

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
		if(id == 2) {
			return true;
		}else {
			return false;
		}
		
	}

}
