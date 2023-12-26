package cn.sjtu.autoupdate.faultlocalization;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;

public class CompilationErrorResolver {
	
	private CategorizedProblem prob;
	public CompilationErrorResolver(CategorizedProblem prob) {
		this.prob = prob;
	}
	public String getErrorCodeName() {
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		
		switch(id) {
			case 2:
			case 390:
			case 50:
				return getUnresolvedType(prob.getMessage());
		}
		
		return null;
	}
	private String getUnresolvedType(String msg) {
		int mark = msg.indexOf(" ");
		return msg.substring(0, mark);
	}

}
