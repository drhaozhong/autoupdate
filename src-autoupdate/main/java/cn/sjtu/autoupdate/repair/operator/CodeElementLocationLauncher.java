package cn.sjtu.autoupdate.repair.operator;

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
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.factory.Factory;

public class CodeElementLocationLauncher extends SpoonLauncher {
	Logger logger = Logger.getLogger(SpoonLocationPointerLauncher.class.getName());
	public static boolean originalLocation = true;
	
	public CodeElementLocationLauncher(Factory factory) throws Exception {
		super(factory);
	}

	
	public List<CtElement> run(CtClass ctclass, String packName) {
		this.addProcessor(CodeElementPointer.class.getName());
		CodeElementPointer.suspElements.clear();
		CodeElementPointer.packName = packName;
		this.process(ctclass);
		return new ArrayList<>(CodeElementPointer.suspElements);
	}
	
	
}
