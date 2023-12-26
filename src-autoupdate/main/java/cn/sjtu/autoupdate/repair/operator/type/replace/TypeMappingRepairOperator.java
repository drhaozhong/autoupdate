package cn.sjtu.autoupdate.repair.operator.type.replace;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import com.db4o.EmbeddedObjectContainer;
import com.db4o.ObjectSet;

import cn.sjtu.autoupdate.apidoc.data.TypeInfo;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.engine.AutoupdateMutationSupporter;
import cn.sjtu.autoupdate.repair.operator.type.TypeRepairOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.StatementOperatorInstance;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.sourcecode.VariableResolver;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.solutionsearch.spaces.operators.AutonomousOperator;
import spoon.reflect.code.BinaryOperatorKind;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;

public class TypeMappingRepairOperator extends TypeReplacerRepairOperator{
	private Hashtable<String, ArrayList<String>> shortmappings;

	public TypeMappingRepairOperator(Hashtable<String, ArrayList<String>> shortmappings) {
		
		this.shortmappings = shortmappings;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();
		String errorName = getErrorCodeName(modificationPoint);
		ArrayList<String> newTypes = shortmappings.get(errorName);
		
		CtElement element = getErrorCode(errorName, modificationPoint.getCodeElement());
		String newType = this.selectNewType(newTypes, errorName, modificationPoint.generation);
		
//		if(errorName.indexOf("TextBox")>=0) {
//			System.out.println("Here");
//		}
		
		if(newType!=null) {
			if(element instanceof CtTypeReferenceImpl) {	
				instances.add(createOperatorInstances(modificationPoint, (CtTypeReferenceImpl)element, newType));
			}else if(element instanceof CtExecutableReferenceImpl) {
				instances.add(createOperatorInstances(modificationPoint, (CtConstructorCall)element.getParent(), newType));
			}
		}
		
		return instances;
	}
	

}
