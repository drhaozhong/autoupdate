package cn.sjtu.autoupdate.repair.operator.method;

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
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.SpoonClassNotFoundException;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.declaration.CtClassImpl;
import spoon.support.reflect.declaration.CtMethodImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;

public class UnimplementRepairOperator extends MethodRepairOperator{
	final Set<String> prim = new HashSet<String>(Arrays.asList("byte", "Byte", "long", "Long", "int", "Integer",
			"float", "Float", "double", "Double", "short", "Short", "char", "Character"));

	

	public UnimplementRepairOperator() {
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();
		
		if(element instanceof CtClassImpl) {
			SuspiciousModificationPoint smp = (SuspiciousModificationPoint)modificationPoint;
			SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
			CategorizedProblem prob = scc.getProblem();
			int mark = prob.getMessage().lastIndexOf("(");
			String methodName = prob.getMessage().substring(0, mark);
			mark = methodName.lastIndexOf(".");
			if(mark>0) {
				methodName = methodName.substring(mark+1);
			}
			
			CtClassImpl oldClass = (CtClassImpl)element;		
			if(oldClass.getSuperclass()!=null) {
				for(Object o:oldClass.getSuperclass().getAllExecutables()) {
					CtExecutableReferenceImpl executable = (CtExecutableReferenceImpl)o;
					String simpleName = executable.getSimpleName();
					if(simpleName.compareTo(methodName)==0) {
						OperatorInstance instance = createOperatorInstances(modificationPoint, oldClass, executable);
						instances.add(instance);
					}
				}
			}
		
			for(Object o:oldClass.getSuperInterfaces()) {
				CtTypeReference inter = (CtTypeReference)o;
				for(Object oo:inter.getAllExecutables()) {
					CtExecutableReferenceImpl executable = (CtExecutableReferenceImpl)oo;
					String simpleName = executable.getSimpleName();
					if(simpleName.compareTo(methodName)==0) {
						OperatorInstance instance = createOperatorInstances(modificationPoint, oldClass, executable);
						instances.add(instance);
					}
				}
			}
		
		}
		return instances;

	};
	
	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtClassImpl oldClass,
			CtExecutableReference executable) {
		
		CtMethodImpl method = new CtMethodImpl();
		method.setSimpleName(executable.getSimpleName());
		
//		if(Modifier.isProtected(executable.getActualMethod().getModifiers())){
//			method.addModifier(ModifierKind.PROTECTED);
//		}
//		if(Modifier.isPublic(executable.getActualMethod().getModifiers())){
			method.addModifier(ModifierKind.PUBLIC);
//		}			
//		for(Class cl:executable.getActualMethod().getExceptionTypes()) {
//			method.addThrownType(MutationSupporter.factory.Type().createReference(cl));
//		}
		
		for(Object o:executable.getReferencedTypes()) {
			CtTypeReference type = (CtTypeReference)o;
			if(type.getSimpleName().endsWith("Exception")) {
				method.addThrownType(type);
			}
		}
			
		for(int i=0; i<executable.getParameters().size(); i++) {
			CtTypeReference type = (CtTypeReference)executable.getParameters().get(i);
			MutationSupporter.factory.Method().createParameter(method, type, "para"+i);
		}
		CtTypeReference rt = executable.getType();
		method.setType(rt);
		CtReturn<Object> returnStatement;
		if (rt == null || "void".equals(rt.getSimpleName())) {
			returnStatement = MutationSupporter.getFactory().Core().createReturn();
		} else {
			String codeExpression = "";
			if (prim.contains(rt.getSimpleName())) {
				codeExpression = getZeroValue(rt.getSimpleName().toLowerCase());
			} else if (rt.getSimpleName().toLowerCase().equals("boolean")) {
				codeExpression = "false";
			} else {
				codeExpression = "null";
			}
			CtExpression returnExpression = MutationSupporter.getFactory().Code()
					.createCodeSnippetExpression(codeExpression);
			returnStatement = MutationSupporter.getFactory().Core().createReturn();
			returnStatement.setReturnedExpression(returnExpression);
		}
		method.setBody(returnStatement);
		
		
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldClass, method, oldClass.getShortRepresentation()+"->"+method.getShortRepresentation());
		return operatorInstance;
	}

	private String getZeroValue(String simpleName) {
		if ("float".equals(simpleName))
			return "0f";
		if ("long".equals(simpleName))
			return "0l";
		if ("double".equals(simpleName))
			return "0d";
		return "0";
	}


	@Override
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtClass ctst = (CtClass) operation.getOriginal();			
			CtMethodImpl fix = (CtMethodImpl) operation.getModified();
			ctst.addMethod(fix);
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
	public boolean undoChangesInModel(OperatorInstance operation, ProgramVariant p) {
		try {
			CtClass ctst = (CtClass) operation.getOriginal();			
			CtMethodImpl fix = (CtMethodImpl) operation.getModified();			
			ctst.removeMethod(fix);
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
		if(id == 400) {
			return true;
		}else {
			return false;
		}
	}

}
