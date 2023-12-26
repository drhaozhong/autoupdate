package cn.sjtu.autoupdate.faultlocalization;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.faultlocalization.bridgeFLSpoon.SpoonElementPointer;
import fr.inria.astor.core.faultlocalization.bridgeFLSpoon.SpoonLauncher;
import fr.inria.astor.core.faultlocalization.bridgeFLSpoon.SpoonLocationPointerLauncher;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import spoon.reflect.code.CtDo;
import spoon.reflect.code.CtFor;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtWhile;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;

public class CompilationErrorLocationPointerLauncher extends SpoonLauncher {
	Logger logger = Logger.getLogger(SpoonLocationPointerLauncher.class.getName());
	public static boolean originalLocation = true;
	
	public CompilationErrorLocationPointerLauncher(Factory factory) throws Exception {
		super(factory);
	}

	/**
	 * Return the ctElement from a line. 
	 * 
	 * @param ctelement
	 * @param candidate
	 * @param onlyRoot
	 * @return ctElements from the line
	 */
	public List<CtElement> run(CtElement ctelement, SuspiciousCompiledCode candidate) {
		this.addProcessor(CompilationElementPointer.class.getName());
		CompilationElementPointer.suspElements.clear();
		CompilationElementPointer.candidate = candidate;
		this.process(ctelement);
		return new ArrayList<>(CompilationElementPointer.suspElements);
	}
	
	
}
