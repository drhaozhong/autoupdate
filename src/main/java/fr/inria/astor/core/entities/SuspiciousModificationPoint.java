package fr.inria.astor.core.entities;

import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousTestedCode;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtVariable;
/**
 * ModificationPoint created from a Suspicious code. 
 * That means, the ModificationPoint is a suspicious to have a bug.
 * @author Matias Martinez,  matias.martinez@inria.fr
 *
 */
public class SuspiciousModificationPoint extends ModificationPoint{

	protected SuspiciousCode suspicious;

	
	public SuspiciousModificationPoint(){
		super();
	}
	
	public SuspiciousModificationPoint(SuspiciousCode suspicious, CtElement rootElement, CtType clonedClass,List<CtVariable> context) {
		super(rootElement, (CtClass) clonedClass, context);
		this.suspicious = suspicious;
	}
	public SuspiciousCode getSuspicious() {
		return suspicious;
	}
	public void setSuspicious(SuspiciousCode suspicious) {
		this.suspicious = suspicious;
	}
	
	public String toString(){
		if(suspicious instanceof SuspiciousCompiledCode) {
			SuspiciousCompiledCode scc = (SuspiciousCompiledCode)suspicious;
			CategorizedProblem prob = scc.getProblem();
			String msg = prob.getMessage();
			return msg;
		}else {
			return "MP="
				+ ctClass.getQualifiedName()+" line: "+suspicious.getLineNumber()+", pointed element: "+codeElement.getClass().getSimpleName()+"";
		}
	}
	
}
