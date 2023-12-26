package cn.sjtu.autoupdate.repair.operator.method;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import spoon.reflect.code.CtBinaryOperator;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtElement;
import spoon.support.reflect.code.CtBinaryOperatorImpl;
import spoon.support.reflect.code.CtBlockImpl;
import spoon.support.reflect.code.CtInvocationImpl;

public class MethodDeleteOperator extends MethodRepairOperator{
	

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();
		CtElement element = modificationPoint.getCodeElement();
		CtInvocationImpl invoc = getInvocation(element);
		if(invoc.getParent() instanceof CtBlockImpl) {
			CtBlockImpl block = (CtBlockImpl)invoc.getParent();
			OperatorInstance instance = createOperatorInstances(modificationPoint, invoc, block);
			instances.add(instance);
		}else if(invoc.getParent() instanceof CtInvocationImpl) {
			if(invoc.getArguments().size()==1) {
				CtInvocationImpl parent = (CtInvocationImpl)invoc.getParent();
				OperatorInstance instance = createOperatorInstances(modificationPoint, invoc, parent);
				instances.add(instance);
			}
		}else if(invoc.getParent() instanceof CtBinaryOperatorImpl) {
			if(invoc.getArguments().size()==1) {
				CtBinaryOperatorImpl parent = (CtBinaryOperatorImpl)invoc.getParent();
				OperatorInstance instance = createOperatorInstances(modificationPoint, invoc, parent);
				if(instance!=null) {
					instances.add(instance);
				}
			}
		}
		return instances;
	}

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtInvocationImpl invoc,
			CtBinaryOperatorImpl oldBinary) {
		CtBinaryOperator newBinary = oldBinary.clone();
		if(newBinary.getLeftHandOperand().toString().compareTo(invoc.toString())==0) {
			newBinary.setLeftHandOperand((CtExpression) invoc.getArguments().get(0));
		}
		if(newBinary.getRightHandOperand().toString().compareTo(invoc.toString())==0) {
			newBinary.setRightHandOperand((CtExpression) invoc.getArguments().get(0));
		}
		if(newBinary.toString().compareTo(oldBinary.toString())!=0) {
			OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldBinary, newBinary, oldBinary.getShortRepresentation()+"->"+newBinary.getShortRepresentation());
			return operatorInstance;
		}else {
			return null;
		}
	}

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtInvocationImpl invoc,
			CtInvocationImpl oldInvoc) {
		CtInvocation newInvoc = oldInvoc.clone();
		List newArgs = newInvoc.getArguments();
		int index = -1;
		for(int i=0; i<newArgs.size(); i++) {
			if(newArgs.get(i).toString().compareTo(invoc.toString())==0) {
				index = i;
				break;
			}
		}
		if(index!=-1) {
			newArgs.set(index, invoc.getArguments().get(0));
		}
		newInvoc.setArguments(newArgs);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldInvoc, newInvoc, oldInvoc.getShortRepresentation()+"->"+newInvoc.getShortRepresentation());
		return operatorInstance; 
	}

	private OperatorInstance createOperatorInstances(ModificationPoint modificationPoint, CtInvocationImpl invoc, CtBlockImpl oldBlock) {
		CtBlock newBlock = oldBlock.clone();
		CtStatement toDel = null;
		for(CtStatement statement:newBlock.getStatements()) {
			if(statement.toString().compareTo(invoc.toString())==0) {
				toDel = statement;
				break;
			}
		}
		newBlock.removeStatement(toDel);
		OperatorInstance operatorInstance = new OperatorInstance(modificationPoint, this, oldBlock, newBlock, oldBlock.getShortRepresentation()+"->"+newBlock.getShortRepresentation());
		return operatorInstance;
	}

	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		try {
			CtElement element = smp.getCodeElement();
			CtInvocationImpl invoc = getInvocation(element);
			if(invoc!=null&&(invoc.getParent() instanceof CtBlockImpl||invoc.getParent() instanceof CtInvocationImpl||invoc.getParent() instanceof CtBinaryOperatorImpl)) {
				return true;
			}else {
				return false;
			}
		}catch(Exception e) {
			return false;
		}
	}
}
