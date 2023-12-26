package cn.sjtu.autoupdate.repair.operator.type.replace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.core.compiler.IProblem;

import cn.sjtu.autoupdate.apidoc.data.TypeItem;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.repair.operator.type.TypeRepairOperator;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.solutionsearch.spaces.operators.AstorOperator;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtFieldReadImpl;
import spoon.support.reflect.code.CtInvocationImpl;
import spoon.support.reflect.reference.CtExecutableReferenceImpl;
import spoon.support.reflect.reference.CtFieldReferenceImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class BestTypeNameRepairOperator extends TypeReplacerRepairOperator {

	
	private ArrayList<TypeItem> classes;
	private int topN;
	private Levenshtein stringComparator = new Levenshtein();
	

	public BestTypeNameRepairOperator(ArrayList<TypeItem> newClasses, int i) {
		this.classes = newClasses;
		this.topN = i;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {
		List<OperatorInstance> instances = new ArrayList<>();
		String errorName = getErrorCodeName(modificationPoint);
		String newType = findNewType(errorName, modificationPoint);
		
//		if(errorName.indexOf("SlideShow")>=0) {
//			System.out.println("Here");
//		}
		CtElement element = getErrorCode(errorName, modificationPoint.getCodeElement());
		if(element instanceof CtFieldReferenceImpl) {
			element = element.getParent();
		}
		if(element==null) {
			element = modificationPoint.getCodeElement();
		}
		if(newType!=null) {
			if(element instanceof CtTypeReferenceImpl) {
				if(element.toString().compareTo(newType)!=0) {
					instances.add(createOperatorInstances(modificationPoint, (CtTypeReferenceImpl)element, newType));
				}
			}else if(element instanceof CtExecutableReferenceImpl) {
				if(element.getParent() instanceof CtConstructorCall) {
					instances.add(createOperatorInstances(modificationPoint, (CtConstructorCall)element.getParent(), newType));
				}else if(element.getParent() instanceof CtInvocationImpl) {
					instances.add(createOperatorInstances(modificationPoint, (CtInvocationImpl)element.getParent(), newType));
				}
			}else if(element instanceof CtFieldReadImpl) {
				instances.add(createOperatorInstances(modificationPoint, (CtFieldReadImpl)element, newType));
			}
		}
		return instances;
	}

	
	



	private String findNewType(String oldName, ModificationPoint modificationPoint) {
		ArrayList<String> newTypes = findTopNmatches(oldName);	
		String newType = selectNewType(newTypes, oldName,  modificationPoint.generation);
		return newType;
	}



	private ArrayList<String> findTopNmatches(String oldName) {
		for(TypeItem type:classes) {
			type.sim = stringComparator.getUnNormalisedSimilarity(type.name, oldName);
		}
		classes.sort(new Comparator<TypeItem>() {
            @Override
            public int compare(TypeItem t1, TypeItem t2) {
            	int value = (int)((t1.sim - t2.sim)*1000);
                return value;
            }
        });
		ArrayList<String> candidates = new ArrayList<String>();
		for(TypeItem type:classes) {
			if(type.sim==0) {
				candidates.add(type.fullname);
			}
		}
		int size = topN<classes.size()?topN:classes.size();
		int count = 0;
		for(TypeItem type:classes) {
			if(count>=size) {
				break;
			}
			if(type.sim!=0) {
				candidates.add(type.fullname);
				count++;
			}
		}
		return candidates;
	}

	@Override
	public boolean canBeAppliedToPoint(ModificationPoint point) {
		SuspiciousModificationPoint smp = (SuspiciousModificationPoint)point;
		
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)smp.getSuspicious();
		CategorizedProblem prob = scc.getProblem();
		int id = prob.getID()& IProblem.IgnoreCategoriesMask;
		if(id == 2||id==390||id==50||id==83) {
			return true;
		}else {
			return false;
		}
		
	}
	
	

}
