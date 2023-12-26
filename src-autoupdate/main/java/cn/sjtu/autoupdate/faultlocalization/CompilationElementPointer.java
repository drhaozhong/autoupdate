package cn.sjtu.autoupdate.faultlocalization;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.faultlocalization.bridgeFLSpoon.SpoonElementPointer;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import spoon.processing.AbstractProcessor;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;

public class CompilationElementPointer  extends AbstractProcessor<CtElement>{
	Logger logger = Logger.getLogger(SpoonElementPointer.class.getName());
	
	/**
	 * Result of the processor: CtElements found in line given by attribute @line
	 */
	public static List<CtElement> suspElements = new ArrayList<CtElement>();


	public static SuspiciousCompiledCode candidate;
	
	
	public void process(CtElement element) {
		SourcePosition pos = element.getPosition();
		if (pos != null && pos.isValidPosition() ) {
			if(isWithinProb(element)) {
				suspElements.add(element);
			}
		}else {
			CtElement parent = getValidParent(element);
			if(isWithinProb(parent)) {
				suspElements.add(element);
			}
		}
	}


	private CtElement getValidParent(CtElement element) {
		CtElement parent = element.getParent();
		while(!parent.getPosition().isValidPosition()) {
			parent = parent.getParent();
		}
		return parent;
	}


	private boolean isWithinProb(CtElement element) {
		int SourceStart = element.getPosition().getSourceStart();
		int SourceEnd = element.getPosition().getSourceEnd();
		int ProbStart = candidate.getProblem().getSourceStart();
		int ProbEnd = candidate.getProblem().getSourceEnd();
		if((SourceStart <= ProbStart) && (SourceEnd >= ProbStart)||	(SourceStart >= ProbStart) && (SourceStart <= ProbEnd)) {
			return true;
		}else {
			return false;
		}
	}
}
