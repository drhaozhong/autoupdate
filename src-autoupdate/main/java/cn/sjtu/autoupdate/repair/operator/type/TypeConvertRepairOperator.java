package cn.sjtu.autoupdate.repair.operator.type;

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
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.compiler.jdt.ContextBuilder.CastInfo;
import spoon.support.reflect.code.CtAssignmentImpl;
import spoon.support.reflect.code.CtCaseImpl;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtLocalVariableImpl;
import spoon.support.reflect.code.CtReturnImpl;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;

public class TypeConvertRepairOperator extends TypeRepairOperator{
	
	public TypeConvertRepairOperator() {
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();
		if(element instanceof CtTypeAccessImpl) {
			element = element.getParent();
		}
		if(element instanceof CtFieldReadImpl) {
			CtFieldReadImpl fr = (CtFieldReadImpl)element;
			CtTypeReference type = null;
			if(fr.getParent() instanceof CtAssignmentImpl) {
				CtAssignmentImpl ass = (CtAssignmentImpl)fr.getParent();
				CtExpression assinged = ass.getAssigned();
				type = assinged.getType();
			}else if(fr.getParent() instanceof CtReturnImpl) {
				CtElement parent = fr.getParent();
				while(!(parent instanceof CtMethodImpl)) {
					parent = parent.getParent();
				}
				CtMethodImpl method = (CtMethodImpl)parent;
				type = method.getType();
			}else if(fr.getParent() instanceof CtLocalVariableImpl) {
				CtLocalVariableImpl lv = (CtLocalVariableImpl)fr.getParent();
				type = lv.getReference().getType();
			}else if(fr.getParent() instanceof CtInvocationImpl) {
				CtInvocationImpl inv = (CtInvocationImpl)fr.getParent();
				for(int i=0; i<inv.getArguments().size(); i++) {
					Object arg = inv.getArguments().get(i);
					if(arg.toString().compareTo(fr.toString())==0) {
						Object para = inv.getExecutable().getParameters().get(i);
						type = (CtTypeReference)para;
					}
				}
				if(type == null) {
					CtElement parent = inv.getParent();
					if(parent instanceof CtLocalVariableImpl) {
						CtLocalVariableImpl varDef = (CtLocalVariableImpl)parent;
						type = varDef.getType();
						element =  element.getParent();
					}
				}
			}
			if(type!=null) {
				instances.add(createOperatorInstances(modificationPoint, element, type));
			}
		}
		return instances;

	};
	
	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtElement element,
			CtTypeReference type) {
		OperatorInstance instance = null;
		if(element instanceof CtFieldReadImpl) {
			instance = createOperatorInstances(modificationPoint, (CtFieldReadImpl)element, type);
		}else if(element instanceof CtInvocationImpl) {
			instance = createOperatorInstances(modificationPoint, (CtInvocationImpl)element, type);
		}
		return instance;
	}

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtInvocationImpl fr, CtTypeReference type) {
		CtTypeReference cast = type.clone();
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, fr, cast, fr.getShortRepresentation()+"->"+cast.getShortRepresentation());
		return operatorInstance;
	}
	
	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtFieldReadImpl fr, CtTypeReference type) {
		CtTypeReference cast = type.clone();
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, fr, cast, fr.getShortRepresentation()+"->"+cast.getShortRepresentation());
		return operatorInstance;
	}

	
	@Override
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtTypeReference fix = (CtTypeReference) operation.getModified();
			List types = new ArrayList<CtTypeReference>();
			types.add(fix);
			
			if(operation.getOriginal() instanceof CtFieldReadImpl) {
				CtFieldReadImpl ctst = (CtFieldReadImpl) operation.getOriginal();
				ctst.setTypeCasts(types);
				log.debug(" applied: " + ctst.getParent().toString());
			}else if(operation.getOriginal() instanceof CtInvocationImpl) {
				CtInvocationImpl ctst = (CtInvocationImpl)operation.getOriginal();
				ctst.setTypeCasts(types);
				log.debug(" applied: " + ctst.getParent().toString());
			}
			
			successful = true;
			operation.setSuccessfulyApplied((successful));
			
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
			CtTypeReference fix = (CtTypeReference) operation.getModified();	
			List types = new ArrayList<CtTypeReference>();
			if(operation.getOriginal() instanceof CtFieldReadImpl) {
				CtFieldReadImpl ctst = (CtFieldReadImpl) operation.getOriginal();
				ctst.setTypeCasts(types);
			}else if(operation.getOriginal() instanceof CtInvocationImpl) {
				CtInvocationImpl ctst = (CtInvocationImpl) operation.getOriginal();
				ctst.setTypeCasts(types);
			}
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
		if(id == 580||id == 19||id == 17) {
			return true;
		}else {
			return false;
		}
	}

}
