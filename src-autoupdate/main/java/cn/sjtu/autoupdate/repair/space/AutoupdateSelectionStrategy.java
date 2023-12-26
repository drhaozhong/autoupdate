package cn.sjtu.autoupdate.repair.space;

import java.util.ArrayList;

import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.RandomManager;
import fr.inria.astor.core.solutionsearch.spaces.operators.AstorOperator;
import fr.inria.astor.core.solutionsearch.spaces.operators.OperatorSelectionStrategy;
import fr.inria.astor.core.solutionsearch.spaces.operators.OperatorSpace;

public class AutoupdateSelectionStrategy extends OperatorSelectionStrategy {

	public AutoupdateSelectionStrategy(OperatorSpace space) {
		super(space);
		// TODO Auto-generated constructor stub
	}

	@Override
	public AstorOperator getNextOperator() {
		OperatorSpace space = getOperatorSpace();
		AstorOperator[] operators = space.values();
		return operators[RandomManager.nextInt(operators.length)];
	}

	/**
	 * Given a suspicious modif point, the method randomly decides to mutate it according to its suspicious
	 * @param modificationPoint
	 * @return
	 */
	protected boolean mutateModificationPoint(SuspiciousModificationPoint modificationPoint) {

		if (ConfigurationProperties.getPropertyBool("probagenmutation")) {
			double randomVal = RandomManager.nextDouble();
			
			double suspiciousValue = modificationPoint.getSuspicious().getSuspiciousValue();
			return ((suspiciousValue * ConfigurationProperties.getPropertyDouble("mutationrate")) 
					>= randomVal);

		}
		//By default, we mutate the point
		return true;
	}

	@Override
	public AstorOperator getNextOperator(SuspiciousModificationPoint modificationPoint) {
		
		//If we decide to mutate the point according to its suspiciousness value
		if(mutateModificationPoint(modificationPoint)){
			//here, this strategy does not take in account the modifpoint to select the op.
			return this.getNextCanApplyOperator(modificationPoint);
		}
		else{
			//We dont mutate the modif point
			return null;
		}
		
		
	}

	private AstorOperator getNextCanApplyOperator(SuspiciousModificationPoint modificationPoint) {
		OperatorSpace space = getOperatorSpace();
		ArrayList<AstorOperator> operators = new ArrayList<AstorOperator>(); 
		for(AstorOperator op:space.values()) {
			if(op.canBeAppliedToPoint(modificationPoint)) {
				operators.add(op);
			}
		}
		if(operators.size()>0) {
			return operators.get(RandomManager.nextInt(operators.size()));
		}else {
			return null;
		}
	}

}
