package fr.inria.astor.core.solutionsearch.navigation;

import java.util.Comparator;
import java.util.List;

import org.junit.Assert;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousTestedCode;

/**
 * 
 * @author Matias Martinez
 *
 */
public class InOrderSuspiciousNavigation implements SuspiciousNavigationStrategy {

	/**
	 * same order the MP are referenced by the variant.
	 */
	@Override
	public List<ModificationPoint> getSortedModificationPointsList(List<ModificationPoint> modificationPoints) {

		modificationPoints.sort(new Comparator<ModificationPoint>() {

			@Override
			public int compare(ModificationPoint o1, ModificationPoint o2) {
				SuspiciousCode s1 = ((SuspiciousModificationPoint) o1).getSuspicious();
				SuspiciousCode s2 = ((SuspiciousModificationPoint) o2).getSuspicious();
				int value = 0;
				if(s1 instanceof SuspiciousTestedCode) {
					SuspiciousTestedCode stc1 = (SuspiciousTestedCode)s1;
					SuspiciousTestedCode stc2 = (SuspiciousTestedCode)s2;
					value = Double.compare(stc1.getSuspiciousValue(),  stc2.getSuspiciousValue());
				}else if(s1 instanceof SuspiciousCompiledCode) {
					SuspiciousCompiledCode scc1 = (SuspiciousCompiledCode)s1;
					SuspiciousCompiledCode scc2 = (SuspiciousCompiledCode)s2;
					value = Integer.compare(scc1.getLineNumber(), scc2.getLineNumber()); 
				}
				return value;
			}
		});
		return modificationPoints;
	}

}
