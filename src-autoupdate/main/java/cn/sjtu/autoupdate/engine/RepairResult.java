package cn.sjtu.autoupdate.engine;

import java.util.ArrayList;
import java.util.List;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;

public class RepairResult {
	public ArrayList<String> errors;
	public int id;
	
	
	public RepairResult(List initSuspList) {
		errors = new ArrayList<String>();
		for(Object o:initSuspList) {
			SuspiciousCompiledCode sc = (SuspiciousCompiledCode)o;
			errors.add(sc.getProblem().getMessage());
		}
	}
	public RepairResult() {
		// TODO Auto-generated constructor stub
	}
	
	public void setResult(List<SuspiciousCompiledCode> tmpSuspicousList, int g) {
		id = g;
		errors = new ArrayList<String>();
		for(SuspiciousCompiledCode sc:tmpSuspicousList) {
			errors.add(sc.getProblem().getMessage());
		}
		
	}
	public double getFitness() {
		// TODO Auto-generated method stub
		return errors.size();
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return errors.size()+"";
	}	
}
