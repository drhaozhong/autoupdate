package cn.sjtu.autoupdate.repair.operator.type.replace;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

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
import spoon.reflect.declaration.CtElement;
import spoon.reflect.reference.CtPackageReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.support.reflect.code.CtTypeAccessImpl;
import spoon.support.reflect.reference.CtTypeReferenceImpl;
import uk.ac.shef.wit.simmetrics.similaritymetrics.Levenshtein;

public class AmbiguousTypeRepairOperator extends TypeReplacerRepairOperator {

	private ArrayList<TypeItem> classes;
	
	private Levenshtein stringComparator = new Levenshtein();

	public AmbiguousTypeRepairOperator(ArrayList<TypeItem> newClasses) {
		this.classes = newClasses;
	}

	@Override
	public List<OperatorInstance> createOperatorInstances(ModificationPoint modificationPoint) {

		List<OperatorInstance> instances = new ArrayList<>();
		
		CtElement element = modificationPoint.getCodeElement();
		
		if(element instanceof CtTypeAccessImpl) {
			CtTypeAccessImpl cti = (CtTypeAccessImpl)element;
			CtTypeReferenceImpl oldType = (CtTypeReferenceImpl)cti.getAccessedType();
			
			ArrayList<String> newTypes = findMatches(oldType);
			String newType = this.selectNewType(newTypes, oldType.getSimpleName(), modificationPoint.generation);
			instances.add(createOperatorInstances(modificationPoint, oldType, newType));
		}
		return instances;

	}

	private ArrayList<String> findMatches(CtTypeReferenceImpl oldType) {
		String oldName = oldType.getSimpleName();
		ArrayList<String> candidates = new ArrayList<String>();
		for(TypeItem type:classes) {
			if(type.name.compareTo(oldName)==0) {
				candidates.add(type.fullname);
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
		if(id == 4) {
			return true;
		}else {
			return false;
		}
		
	}
	
	@Override
	public boolean applyChangesInModel(OperatorInstance operation, ProgramVariant p) {
		boolean successful = false;
		try {
			CtTypeReferenceImpl ctst = (CtTypeReferenceImpl)operation.getOriginal();			
			CtTypeReferenceImpl fix = (CtTypeReferenceImpl)operation.getModified();		
			CtPackageReference oldPack = ctst.getPackage();
			ctst.setPackage(fix.getPackage());
			fix.setPackage(oldPack);
			successful = true;
			operation.setSuccessfulyApplied((successful));
			log.debug(" applied: " + ctst.toString());
		} catch (Exception ex) {
			log.error("Error applying an operation, exception: " + ex.getMessage());
			operation.setExceptionAtApplied(ex);
			operation.setSuccessfulyApplied(false);
		}
		return successful;
	}

	
	@Override
	public boolean undoChangesInModel(OperatorInstance opInstance, ProgramVariant p) {
		
		try {
			CtTypeReferenceImpl ctst = (CtTypeReferenceImpl)opInstance.getOriginal();
			CtTypeReferenceImpl fix = (CtTypeReferenceImpl)opInstance.getModified();
			
			CtPackageReference oldPack = ctst.getPackage();
			ctst.setPackage(fix.getPackage());
			fix.setPackage(oldPack);

			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
