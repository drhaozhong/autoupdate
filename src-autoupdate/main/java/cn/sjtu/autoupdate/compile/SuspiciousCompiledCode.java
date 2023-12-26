package cn.sjtu.autoupdate.compile;

import java.util.ArrayList;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;

public class SuspiciousCompiledCode extends SuspiciousCode {
	protected CategorizedProblem problem;
	
	public SuspiciousCompiledCode(String fileName, String className, String methodName, int lineNumber, CategorizedProblem prob) {
		super();
		this.fileName = fileName;
		this.className = className;
		this.methodName = methodName;
		this.lineNumber = lineNumber;
		this.problem = prob;
	}

	public CategorizedProblem getProblem() {
		return problem;
	}

	@Override
	public double getSuspiciousValue() {
		return lineNumber;
	}

	@Override
	public String getSuspiciousValueString() {
		return lineNumber+"";
	}

	@Override
	public String toString() {
		return problem.toString();
	}
}
