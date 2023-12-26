package cn.sjtu.autoupdate.repair.operator;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.faultlocalization.bridgeFLSpoon.SpoonElementPointer;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtFieldRead;
import spoon.reflect.code.CtVariableRead;
import spoon.reflect.code.CtVariableWrite;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtExecutableReference;
import spoon.reflect.reference.CtTypeReference;

public class CodeElementPointer  extends AbstractProcessor<CtElement>{
	Logger logger = Logger.getLogger(SpoonElementPointer.class.getName());
	
	/**
	 * Result of the processor: CtElements found in line given by attribute @line
	 */
	public static List<CtElement> suspElements = new ArrayList<CtElement>();
	public static String packName;
	
	
	public void process(CtElement element) {
		String value = null;
		if(element instanceof CtExecutableReference) {
			value = element.toString();
		}else if(element instanceof CtTypeReference) {
			value = element.toString();
		}else if(element instanceof CtFieldRead) {
			value = element.toString();
		}else if(element instanceof CtVariableRead) {
			value = element.toString();
		}else if(element instanceof CtVariableWrite) {
			value = element.toString();
		}
		if(value!=null&&value.indexOf(packName)>=0) {
			suspElements.add(element);
		}
	}
	
}
