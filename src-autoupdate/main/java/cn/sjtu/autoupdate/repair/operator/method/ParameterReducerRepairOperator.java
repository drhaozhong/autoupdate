package cn.sjtu.autoupdate.repair.operator.method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.apidoc.data.MethodItem;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtConstructorCallImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.code.CtVariableReadImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

public class ParameterReducerRepairOperator extends MethodRepairOperator{
	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();		
		CtElement element = modificationPoint.getCodeElement();	
		CtElement oldInvoc = null;
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)modificationPoint;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		
		if(prob.getMessage().indexOf("constructor")>0) {
			String typeName = prob.getMessage().substring(16);
			int mark = typeName.indexOf("(");
			typeName = typeName.substring(0, mark);
			oldInvoc = getConstructor(element, typeName);
			
		}else {
			oldInvoc = getInvocation(element);
			if(oldInvoc==null) {
				oldInvoc = getConstructor(element);
			}
		}
		if(oldInvoc instanceof CtConstructorCallImpl) {
			CtConstructorCallImpl constructor = (CtConstructorCallImpl)oldInvoc;
			try {
				constructor.getParent();
				List args = constructor.getArguments();
				ArrayList<CtExecutableReference> candidates = findCadidates(args, constructor.getExecutable().getSimpleName(), constructor.getType().getAllExecutables());
				
				for(CtExecutableReference executable:candidates) {
					OperatorInstance instance = createOperatorInstance(modificationPoint, constructor, executable);
					instances.add(instance);
				}
			}catch(Exception e) {
				
			}
		}else if(oldInvoc instanceof CtInvocation) {
			CtInvocation invoc = (CtInvocation)oldInvoc;
			List args = invoc.getArguments();
			ArrayList<CtExecutableReference> candidates = findCadidates(args, invoc.getExecutable().getSimpleName(), invoc.getType().getAllExecutables());
			for(CtExecutableReference executable:candidates) {
				OperatorInstance instance = createOperatorInstance(modificationPoint, invoc, executable);
				instances.add(instance);
			}
		}
		return instances;
	};
	




	private OperatorInstance createOperatorInstance(ModificationPoint modificationPoint, CtInvocation invoc,
			CtExecutableReference executable) {
		CtInvocation<Object> newInvoc = MutationSupporter.factory.createInvocation();
		newInvoc.setExecutable(executable);

		if(executable.isStatic()) {
			CtTypeAccessImpl exp = new CtTypeAccessImpl();
			exp.setAccessedType(executable.getDeclaringType());
			newInvoc.setTarget(exp);
		}else {
			newInvoc.setTarget(invoc.getTarget());
		}
		List parameters = findParameters(invoc.getArguments(), executable.getParameters());
		newInvoc.setArguments(parameters);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, invoc, newInvoc, invoc.getShortRepresentation()+"->"+newInvoc.getShortRepresentation());
		return operatorInstance;
	}






	private List findParameters(List arguments, List parameters) {
		ArrayList args = new ArrayList();
		for(Object o1:parameters) {
			CtTypeReference t1 = (CtTypeReference)o1;
			boolean bFound = false;
			for(Object o2:arguments) {
				String t2 = this.getReturnType(o2);
				if(t1.getSimpleName().indexOf(t2)>=0||t2.indexOf(t1.getSimpleName())>=0) {
					args.add(o2);
					bFound = true;
					break;
				}
			}
			if(!bFound) {
				CtExpression expression = MutationSupporter.getFactory().Code()
						.createCodeSnippetExpression("null");
				args.add(expression);
			}
		}
		
		return args;
	}







	private OperatorInstance createOperatorInstance(ModificationPoint modificationPoint,
			CtConstructorCallImpl constructor, CtExecutableReference executable) {
		CtConstructorCall<Object> newInvoc = constructor.clone();
		newInvoc.setExecutable(executable);
		List parameters = findParameters(constructor.getArguments(), executable.getParameters());
		newInvoc.setArguments(parameters);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, constructor, newInvoc, constructor.getShortRepresentation()+"->"+newInvoc.getShortRepresentation());
		return operatorInstance;
	}






	private ArrayList<CtExecutableReference> findCadidates(List args,
			String name, Collection<CtExecutableReference<?>> allExecutables) {
		ArrayList<CtExecutableReference> candidates = new ArrayList<CtExecutableReference>();
		int max = -1;
		CtExecutableReference match = null;
		for(CtExecutableReference executable:allExecutables) {			
			if(executable.getSimpleName().compareTo(name)==0&&executable.getParameters().size()<=args.size()) {
				ArrayList<String> newTypes = new ArrayList<String>();
				for(Object o:executable.getParameters()) {
					CtTypeReferenceImpl type = (CtTypeReferenceImpl)o;
					newTypes.add(type.getSimpleName());
				}
				ArrayList<String> oldTypes = new ArrayList<String>();
				for(Object o:args) {
					String type = this.getReturnType(o);
					if(type!=null) {
						oldTypes.add(type);
					}
				}
				
				int count = countSubset(newTypes, oldTypes);
				if(count>max) {
					match = executable;
					max = count;
				}
			}
		}
		if(match!=null) {
			candidates.add(match);
		}
		return candidates;
	}










	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtElement ctst = operation.getOriginal();
			CtElement fix = operation.getModified();	
			List args = null;
			if(ctst instanceof CtConstructorCallImpl) {
				CtConstructorCallImpl conct = (CtConstructorCallImpl)ctst;
				args = conct.getArguments();
				
				CtConstructorCallImpl confix = (CtConstructorCallImpl)fix;
				conct.setArguments(confix.getArguments());
				confix.setArguments(args);
			}else if(ctst instanceof CtInvocationImpl) {
				CtInvocationImpl inovct = (CtInvocationImpl)ctst;
				args = inovct.getArguments();
				CtInvocationImpl inovfix = (CtInvocationImpl)fix;
				args = inovfix.getArguments();
				inovct.setArguments(inovfix.getArguments());
				inovfix.setArguments(args);
			}
//			ctst.replace(fix);
			successful = true;
			operation.setSuccessfulyApplied((successful));
			log.debug(" applied: " + ctst.getParent().toString());
		} catch (Exception ex) {
			log.error("Error applying an operation, exceptimaion: " + ex.getMessage());
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
			List args = null;
			if(ctst instanceof CtConstructorCallImpl) {
				CtConstructorCallImpl conct = (CtConstructorCallImpl)ctst;
				args = conct.getArguments();
				CtConstructorCallImpl confix = (CtConstructorCallImpl)fix;
				conct.setArguments(confix.getArguments());
				confix.setArguments(args);
			}else if(ctst instanceof CtInvocationImpl) {
				CtInvocationImpl inovct = (CtInvocationImpl)ctst;
				args = inovct.getArguments();
				CtInvocationImpl inovfix = (CtInvocationImpl)fix;
				args = inovfix.getArguments();
				inovct.setArguments(inovfix.getArguments());
				inovfix.setArguments(args);
			}
//			fix.replace(ctst);
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
		if((id == 130)||(id==83)||id == 115) {
			return true;
		}else {
			return false;
		}
	}
}
