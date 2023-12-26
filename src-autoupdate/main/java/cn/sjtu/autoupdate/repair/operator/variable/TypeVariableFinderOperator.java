package cn.sjtu.autoupdate.repair.operator.variable;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.apidoc.data.TypeItem;
import cn.sjtu.autoupdate.apidoc.data.VarInfo;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.repair.operator.TypeMatchOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.reference.CtVariableReference;
import spoon.support.reflect.code.CtFieldAccessImpl;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.support.reflect.reference.CtVariableReferenceImpl;

public class TypeVariableFinderOperator extends VariableRepairOperator{

	private ArrayList<TypeItem> newClasses;

	public TypeVariableFinderOperator(ArrayList<TypeItem> newClasses) {
		this.newClasses = newClasses;
	}



	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {

		List<OperatorInstance> instances = new ArrayList<>();
		
		CtElement element = modificationPoint.getCodeElement();
		
		if(element instanceof CtTypeAccessImpl) {
			CtTypeAccessImpl cti = (CtTypeAccessImpl)element;
			ArrayList<VarInfo> fields = findCandidateFields(cti);
			if(fields!=null) {
				for(VarInfo field:fields) {
					instances.add(createOperatorInstances(modificationPoint, cti, field));
				}
			}
		}
		return instances;

	}
	
	

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtTypeAccessImpl access,
			VarInfo var) {
		CtFieldReadImpl field = new CtFieldReadImpl();
		field.setTarget(access.clone());
		CtFieldReferenceImpl variable = new CtFieldReferenceImpl();
		variable.setSimpleName(var.name);
		field.setVariable(variable);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, access, field, access.getShortRepresentation()+"->"+field.getShortRepresentation());
		return operatorInstance;
	}



	private ArrayList<VarInfo> findCandidateFields(CtTypeAccessImpl actualType) {
		String typeName = actualType.getAccessedType().getQualifiedName();
		TypeItem type = null;
		for(TypeItem t:this.newClasses) {
			if(t.fullname.compareTo(typeName)==0) {
				type = t;
				break;
			}
		}
		if(type!=null) {
			return type.consts;
		}else {
			return null;
		}
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
		if(id == 83) {
			return true;
		}else {
			return false;
		}
	}

}
