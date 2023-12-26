package cn.sjtu.autoupdate.repair.operator.variable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.solutionsearch.spaces.operators.AutonomousOperator;
import spoon.reflect.code.CtExpression;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtVariableReadImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;
import spoon.support.reflect.reference.CtParameterReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.support.reflect.reference.CtVariableReferenceImpl;

public class FieldToMethodRepairOperator extends VariableRepairOperator{

	private Hashtable<String, String> fieldTypes;

	public FieldToMethodRepairOperator(Hashtable<String, String> fieldTypes) {
		this.fieldTypes = fieldTypes;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();
		
		if(element instanceof CtFieldReadImpl) {
			CtFieldReadImpl oldFr = (CtFieldReadImpl)element;			
			OperatorInstance instance = createOperatorInstances(modificationPoint, oldFr);
			if(instance!=null) {
				instances.add(instance);
			}			
		}else if(element instanceof CtInvocationImpl) {
			CtInvocationImpl invoke = (CtInvocationImpl)element;
		
			instances = createOperatorInstances(modificationPoint, invoke);
			
		}
		return instances;

	}

	
	private List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint,
			CtInvocationImpl invoke) {
		List<OperatorInstance> instances = new ArrayList<>();		
		for(Object o:invoke.getArguments()) {
			CtTypeReference type = null;
			if(o instanceof CtFieldReadImpl) {
				CtFieldReadImpl oldFr = (CtFieldReadImpl)o;
				type = oldFr.getTarget().getType();
			}else if(o instanceof CtVariableReadImpl) {
				CtVariableReadImpl oldVar = (CtVariableReadImpl)o;
				type = oldVar.getVariable().getType();
			}
			if(type!=null) {
				List paras = invoke.getExecutable().getParameters();
				for(CtExecutableReference executable: type.getDeclaredExecutables()) {
					if(executable.getExecutableDeclaration() instanceof CtMethodImpl) {
						CtMethodImpl method = (CtMethodImpl) executable.getExecutableDeclaration();
						String t1 = method.getType().getQualifiedName();
						for(Object para:paras) {
							if(para instanceof CtTypeReferenceImpl) {
								CtTypeReferenceImpl paraType = (CtTypeReferenceImpl)para;
								String t2 = paraType.getQualifiedName();
								if(t2!=null&&method.getParameters().size()==0&&t1.compareTo(t2)==0) {
									OperatorInstance instance = createOperatorInstances(modificationPoint, o, executable);
									if(instance!=null) {
										instances.add(instance);
									}
								}	
							}
						}							
					}
				}			
			}
		}
		return instances;
	}
	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, Object o,
			CtExecutableReference executable) {
		OperatorInstance instance = null;
		if(o instanceof CtFieldReadImpl) {
			instance = createOperatorInstances(modificationPoint, (CtFieldReadImpl)o, executable);
		}else if(o instanceof CtVariableReadImpl) {
			instance = createOperatorInstances(modificationPoint, (CtVariableReadImpl)o, executable);
		}
		return instance;
	}

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtFieldReadImpl oldFr,
			CtExecutableReference executable) {
		CtInvocationImpl inv = new CtInvocationImpl<>();
		inv.setTarget(oldFr.getTarget());
		inv.setExecutable(executable);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldFr, inv, oldFr.getShortRepresentation()+"->"+inv.getShortRepresentation());
		return operatorInstance;
	}

	
	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtVariableReadImpl oldVar,
			CtExecutableReference executable) {
		CtInvocationImpl inv = new CtInvocationImpl<>();
		CtVariableReadImpl newVarRead = new CtVariableReadImpl();
		newVarRead.setType(oldVar.getType());
		CtParameterReferenceImpl newVariable = new CtParameterReferenceImpl();
		newVariable.setType(oldVar.getType());
		newVariable.setSimpleName(oldVar.getVariable().getSimpleName());
		newVarRead.setVariable(newVariable);
		inv.setTarget(newVarRead);
		inv.setExecutable(executable);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldVar, inv, oldVar.getShortRepresentation()+"->"+inv.getShortRepresentation());
		return operatorInstance;
	}
	

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, 
			CtFieldReadImpl oldFr) {
		OperatorInstance instance = null;
		if(oldFr.getTarget().getType()!=null) {
			for(CtExecutableReference executable: oldFr.getTarget().getType().getDeclaredExecutables()) {
				if(executable.getExecutableDeclaration() instanceof CtMethodImpl) {
					CtMethodImpl method = (CtMethodImpl) executable.getExecutableDeclaration();
					String t1 = method.getType().getQualifiedName();
					String key = oldFr.getVariable().getQualifiedName().replace("#", ".");
					String t2 = fieldTypes.get(key);
					if(t2!=null&&method.getParameters().size()==0&&t1.compareTo(t2)==0) {
						instance = createOperatorInstances(modificationPoint, oldFr, executable);
					}					
				}
			}				
		}
		return instance;
	};
	
	

	

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
			CtElement ctst = opInstance.getOriginal();
			CtElement fix = opInstance.getModified();
			
			fix.replace(ctst);

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
		if(id == 115) {
			return true;
		}else {
			return false;
		}
	}
}
