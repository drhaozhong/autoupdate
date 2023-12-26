package cn.sjtu.autoupdate.repair.operator.type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.apidoc.data.TypeItem;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.faultlocalization.CompilationErrorLocationPointerLauncher;
import cn.sjtu.autoupdate.repair.operator.CodeElementLocationLauncher;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

public class InvalidTypeRemover extends TypeRepairOperator {
	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();
		String errorName = getErrorCodeName(modificationPoint);
		CtClass element = getClass(modificationPoint.getCodeElement());
		OperatorInstance instance = createOperatorInstances(modificationPoint, element, errorName);
		if(instance!=null) {
			instances.add(instance);
		}
		return instances;
	}

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtClass oldClass,
			String packName) {
		CodeElementLocationLauncher errorlocationlauncher;
		OperatorInstance instance = null;
		try {
			errorlocationlauncher = new CodeElementLocationLauncher(MutationSupporter.getFactory());
			CtClass newClass = oldClass.clone();
			List<CtElement> elements = errorlocationlauncher.run(newClass, packName);
			for(CtElement element:elements) {
				removePackName(element, packName);
			}
			instance = new OperatorInstance(modificationPoint, this, oldClass, newClass, packName+"-> null");

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return instance;
	}


	private void removePackName(CtElement element, String packName) {
		if(element instanceof CtExecutableReference) {
			CtExecutableReference exe = (CtExecutableReference)element;
			if(exe.getDeclaringType()!=null&&exe.getDeclaringType().getQualifiedName().indexOf(packName)>=0){
				exe.setDeclaringType(null);
			}
		}else if(element instanceof CtTypeReference) {
			CtTypeReference type = (CtTypeReference)element;
			if(type.getQualifiedName().indexOf(packName)>=0) {
				type.setPackage(null);
			}
		}else if(element instanceof CtFieldRead) {
			CtFieldRead field = (CtFieldRead)element;
			CtTypeReference type = field.getTarget().getType();
			if(type.getQualifiedName().indexOf(packName)>=0) {
				type.setPackage(null);
				field.getTarget().setType(type);
			}
		}else if(element instanceof CtVariableRead) {
			CtVariableRead var = (CtVariableRead)element;
			CtTypeReference type = var.getType();
			if(type.getQualifiedName().indexOf(packName)>=0) {
				type.setPackage(null);
				var.setType(type);
			}
		}else if(element instanceof CtVariableWrite) {
			CtVariableWrite var = (CtVariableWrite)element;
			CtTypeReference type = var.getType();
			if(type.getQualifiedName().indexOf(packName)>=0) {
				type.setPackage(null);
				var.setType(type);
			}
		}
	}

	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		String errorName = getErrorCodeName(point);
		if(id == 2&&errorName.indexOf(".")>0) {
//		if(id == 2) {
			return true;
		}else {
			return false;
		}
		
	}
}
