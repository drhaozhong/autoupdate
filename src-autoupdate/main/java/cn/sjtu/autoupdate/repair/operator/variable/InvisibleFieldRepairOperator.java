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
import spoon.reflect.reference.CtVariableReference;
import spoon.support.reflect.code.CtAssignmentImpl;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtFieldWriteImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtVariableReadImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.declaration.CtParameterImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;
import spoon.support.reflect.reference.CtLocalVariableReferenceImpl;
import spoon.support.reflect.reference.CtVariableReferenceImpl;

public class InvisibleFieldRepairOperator extends VariableRepairOperator{

	private Hashtable<String, String> fieldTypes;

	public InvisibleFieldRepairOperator(Hashtable<String, String> fieldTypes) {
		this.fieldTypes = fieldTypes;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();
		
		if(element instanceof CtFieldReadImpl) {
			CtFieldReadImpl oldFr = (CtFieldReadImpl)element;
			
			if(oldFr.getVariable().getDeclaringType()!=null) {
				for(CtExecutableReference executable: oldFr.getVariable().getDeclaringType().getDeclaredExecutables()) {
					if(executable.getExecutableDeclaration() instanceof CtMethodImpl) {
						CtMethodImpl method = (CtMethodImpl) executable.getExecutableDeclaration();
						String t1 = method.getType().getSimpleName();
						String key = oldFr.getVariable().getQualifiedName().replace("#", ".");
						String t2 = fieldTypes.get(key);
						if(t2!=null&&method.getParameters().size()==0&&t1.compareTo(t2)==0) {
							instances.add(createOperatorReadInstances(modificationPoint, oldFr, executable));
						}		
					}
				}
			}
		}
		
		if(element instanceof CtFieldWriteImpl) {
			CtFieldWriteImpl oldFw = (CtFieldWriteImpl)element;
			
			if(oldFw.getVariable().getDeclaringType()!=null) {
				for(CtExecutableReference executable: oldFw.getVariable().getDeclaringType().getDeclaredExecutables()) {
					if(executable.getExecutableDeclaration() instanceof CtMethodImpl) {
						CtMethodImpl method = (CtMethodImpl) executable.getExecutableDeclaration();
						String key = oldFw.getVariable().getQualifiedName().replace("#", ".");
						String t2 = fieldTypes.get(key);
						if(t2!=null&&method.getParameters().size()==1) {
							CtParameterImpl para = (CtParameterImpl)method.getParameters().get(0);
							String t1 = para.getType().getQualifiedName();
							if(t1.compareTo(t2)==0) {
								instances.add(createOperatorWriteInstances(modificationPoint, oldFw, executable));
							}		
						}
					}
				}
			}
		}
		return instances;

	};
	
	private OperatorInstance createOperatorWriteInstances(ModificationPoint modificationPoint, CtFieldWriteImpl oldFw,
			CtExecutableReference executable) {
		CtInvocationImpl inv = new CtInvocationImpl<>();
		inv.setExecutable(executable);
		inv.setTarget(oldFw.getTarget());
		CtAssignmentImpl assign = (CtAssignmentImpl)oldFw.getParent();
		ArrayList<CtExpression> args = new ArrayList<CtExpression>();
		args.add(assign.getAssignment());
		inv.setArguments(args);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldFw, inv, oldFw.getShortRepresentation()+"->"+inv.getShortRepresentation());
		return operatorInstance;
	}

	private OperatorInstance createOperatorReadInstances(ModificationPoint modificationPoint, CtFieldReadImpl oldFr,
			CtExecutableReference executable) {
		CtInvocationImpl inv = new CtInvocationImpl<>();
		inv.setExecutable(executable);
		inv.setTarget(oldFr.getTarget());
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldFr, inv, oldFr.getShortRepresentation()+"->"+inv.getShortRepresentation());
		return operatorInstance;
	}

	

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
		if(id == 71) {
			return true;
		}else {
			return false;
		}
	}
	
}
