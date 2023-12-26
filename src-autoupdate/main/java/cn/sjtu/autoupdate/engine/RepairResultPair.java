package cn.sjtu.autoupdate.engine;

import java.util.ArrayList;
import java.util.List;

import fr.inria.astor.core.entities.ProgramVariant;

public class RepairResultPair {
	public RepairResult beforeRepair;
	public RepairResult afterRepair;

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return beforeRepair+"->"+afterRepair;
	}

	public void createNullResult() {
		beforeRepair = new RepairResult();
		beforeRepair.errors = new ArrayList<String>();
		afterRepair = new RepairResult();
		afterRepair.errors = new ArrayList<String>();
	}	
}
