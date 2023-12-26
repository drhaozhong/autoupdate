package cn.sjtu.autoupdate.repair.operator.method;

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
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import spoon.support.visitor.SignaturePrinter;

public class MethodMappingRepairOperator extends MethodRepairOperator{
	private Hashtable<String, String> codemappings;
	private Hashtable<String, ArrayList<String>> shortmappings;

	public MethodMappingRepairOperator(Hashtable<String, String> codemappings, Hashtable<String, ArrayList<String>> shortmappings) {
		this.codemappings = codemappings;
		this.shortmappings = shortmappings;
	}
	
	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {

		List<OperatorInstance> instances = new ArrayList<>();
		
		CtElement element = modificationPoint.getCodeElement();
		
		if(element instanceof CtInvocationImpl) {
			CtInvocationImpl oldInvoc = (CtInvocationImpl)element;
			String key = "";
			if(oldInvoc.getExecutable().getType()!=null) {
				key = oldInvoc.getExecutable().getType().getQualifiedName(); 
			}
			key += oldInvoc.getExecutable().getSimpleName();
			
			String newName = codemappings.get(key);
			if(newName!=null) {
				instances.add(createOperatorInstances(modificationPoint, oldInvoc, newName));
			}else if (shortmappings.get(key)!=null){
				for(String ntn:shortmappings.get(key)) {
					instances.add(createOperatorInstances(modificationPoint, oldInvoc, ntn));
				}
			}
		}
		return instances;

	};

}
