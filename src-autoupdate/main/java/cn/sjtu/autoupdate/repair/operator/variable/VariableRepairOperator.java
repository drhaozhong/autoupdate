package cn.sjtu.autoupdate.repair.operator.variable;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.repair.operator.TypeMatchOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

public abstract class VariableRepairOperator extends TypeMatchOperator{
	
	@Override
	public boolean updateProgramVariant(OperatorInstance opInstance, ProgramVariant p) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		if(id == 70) {
			return true;
		}else {
			return false;
		}
	}

}
