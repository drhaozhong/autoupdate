package cn.sjtu.autoupdate.repair.operator.variable;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

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
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtLocalVariableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.support.visitor.SignaturePrinter;

public class FieldMappingRepairOperator extends VariableRepairOperator{
	private Hashtable<String, String> codemappings;
	private Hashtable<String, ArrayList<String>> shortmappings;

	public FieldMappingRepairOperator(Hashtable<String, String> codemappings, Hashtable<String, ArrayList<String>> shortmappings) {
		this.codemappings = codemappings;
		this.shortmappings = shortmappings;
	}
	
	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {

		List<OperatorInstance> instances = new ArrayList<>();
		
		CtElement element = modificationPoint.getCodeElement();
		
		if(element instanceof CtFieldReadImpl) {
			CtFieldReadImpl oldFr = (CtFieldReadImpl)element;
			
			String key = "";
			if(oldFr.getTarget().getType()!=null) {
				key = oldFr.getTarget().getType().getQualifiedName(); 
			}
			key += "."+oldFr.getVariable().getSimpleName();
			
			String newName = codemappings.get(key);
			if(newName!=null) {
				instances.add(createOperatorInstances(modificationPoint, oldFr, newName));
			}else if (shortmappings.get(key)!=null){
				for(String ntn:shortmappings.get(key)) {
					instances.add(createOperatorInstances(modificationPoint, oldFr, ntn));
				}
			}
		}
		return instances;

	};
	
	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtFieldReadImpl oldFr,
			String newName) {
		int mark = newName.lastIndexOf(".");
		String simpleName = newName.substring(mark+1);
		String typeName = newName.substring(0, mark);
		CtFieldReadImpl newFr = new CtFieldReadImpl();
		newFr.setTarget(oldFr.getTarget());
		CtFieldReferenceImpl var = new CtFieldReferenceImpl();
		newFr.setVariable(var);
		var.setSimpleName(simpleName);
		CtTypeReference<Object> type = MutationSupporter.factory.createTypeReference();
		var.setType(type);
		mark = typeName.lastIndexOf(".");
		type.setSimpleName(typeName.substring(mark+1));
		CtPackageReference pack = MutationSupporter.factory.createPackageReference();
		pack.setSimpleName(typeName.substring(0, mark));
		type.setPackage(pack);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldFr, newFr, oldFr.getShortRepresentation()+"->"+newFr.getShortRepresentation());
		return operatorInstance;
	}

	@Override
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtFieldReadImpl ctst = (CtFieldReadImpl) operation.getOriginal();			
			CtFieldReadImpl fix = (CtFieldReadImpl) operation.getModified();
			CtFieldReference tmp = ctst.getVariable();
			ctst.setVariable(fix.getVariable());			
			fix.setVariable(tmp);
			
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
			CtFieldReadImpl ctst = (CtFieldReadImpl) opInstance.getOriginal();
			CtFieldReadImpl fix = (CtFieldReadImpl) opInstance.getModified();
			
			CtFieldReference tmp = fix.getVariable();
			fix.setVariable(ctst.getVariable());			
			ctst.setVariable(tmp);

			return true;
		} catch (Exception e) {
			return false;
		}
	}
}
