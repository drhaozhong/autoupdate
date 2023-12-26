package cn.sjtu.autoupdate.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.problem.DefaultProblem;

import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import cn.sjtu.autoupdate.faultlocalization.CompilationErrorLocationPointerLauncher;
import cn.sjtu.autoupdate.faultlocalization.CompilationErrorResolver;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousTestedCode;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.manipulation.sourcecode.VariableResolver;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.solutionsearch.population.PopulationController;
import fr.inria.astor.core.solutionsearch.population.ProgramVariantFactory;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtInterface;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.CtVariable;

public class AutoupdateVariantFactory extends ProgramVariantFactory {
	private Logger log = Logger.getLogger(Thread.currentThread().getName());
	@Override
	protected List<SuspiciousModificationPoint> createModificationPoints(SuspiciousCode suspiciousCode,
			ProgramVariant progInstance) {
		List<SuspiciousModificationPoint> suspiciousModificationPoints = new ArrayList<SuspiciousModificationPoint>();

		CtType ctclasspointed = resolveCtClass(suspiciousCode.getClassName(), progInstance);
		
		if (ctclasspointed == null) {
			log.info(" Not ctClass for suspicious code " + suspiciousCode);
			return null;
		}
		
		List<CtElement> ctSuspects = null;
		try {
			ctSuspects = retrieveCtElementForSuspectCode(suspiciousCode, ctclasspointed);
			// The parent first, so I inverse the order
			Collections.reverse(ctSuspects);

		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		SuspiciousCompiledCode scc = (SuspiciousCompiledCode)suspiciousCode;
		
		
		// if we are not able to retrieve suspicious CtElements, we return
		if (ctSuspects.isEmpty()) {
			return null;
		}
		
		

		
		CtElement ctToCheck = getErrorCodeElement(ctSuspects, scc);
		
				
		
		if (ctToCheck!=null) {
			// We take the first element for getting the context (as the remaining
			// have the same location, it's not necessary)		
			List<CtVariable> contextOfPoint = VariableResolver.searchVariablesInScope(ctToCheck);
			SuspiciousModificationPoint modifPoint = new SuspiciousModificationPoint();
			modifPoint.setSuspicious(suspiciousCode);
			modifPoint.setCtClass(ctclasspointed);
			modifPoint.setCodeElement(ctToCheck);
			modifPoint.setContextOfModificationPoint(contextOfPoint);
			suspiciousModificationPoints.add(modifPoint);
			String msg = "--ModifPoint:" + ctToCheck.getClass().getSimpleName();
			if(suspiciousCode instanceof SuspiciousTestedCode) {
				SuspiciousTestedCode stc = (SuspiciousTestedCode)suspiciousCode;
				msg += ", suspValue "
						+ stc.getSuspiciousValue() + ",";
			}
			msg += "file "
					+ ((ctToCheck.getPosition().getFile() == null) ? "-null-file-"
							: ctToCheck.getPosition().getFile().getName());
			log.debug(msg);
		}
		return suspiciousModificationPoints;
	}

	
	private CtElement getErrorCodeElement(List<CtElement> ctSuspects, SuspiciousCompiledCode scc) {
		CtElement ctToCheck = null;
		
		CompilationErrorResolver resolver = new CompilationErrorResolver(scc.getProblem());
		String errorCode = resolver.getErrorCodeName();
		
		for(CtElement element:ctSuspects) {
			if(!element.getPosition().isValidPosition()&&errorCode!=null&&element.toString().indexOf(errorCode)>=0) {
				ctToCheck = element;
				return ctToCheck;
			}
		}
		
		
		ctToCheck = ctSuspects.get(0);
		
		for(CtElement element:ctSuspects) {
			if(element.getPosition()!=null&&element.getPosition().isValidPosition()) {
				int SourceStart = element.getPosition().getSourceStart();
				int SourceEnd = element.getPosition().getSourceEnd();
				int ProbStart = scc.getProblem().getSourceStart();
				int ProbEnd = scc.getProblem().getSourceEnd();
				if((SourceStart <= ProbStart) && (SourceEnd >= ProbStart)||	(SourceStart >= ProbStart) && (SourceStart <= ProbEnd)) {
					int length1 = SourceEnd - SourceStart;
					int length2 = ctToCheck.getPosition().getSourceEnd() - ctToCheck.getPosition().getSourceStart();
					if(length1<length2) {
						if(errorCode!=null) {
							if(element.toString().indexOf(errorCode)>=0) {
								ctToCheck = element;
							}
						}else {
							ctToCheck = element;
						}
					}
				}
			}
		}
		return ctToCheck;
	}


	public List<ProgramVariant> createInitialPopulation(List suspiciousList, int maxNumberInstances,
			PopulationController populationControler, ProjectRepairFacade projectFacade) throws Exception {

		this.projectFacade = projectFacade;

		List<ProgramVariant> variants = new ArrayList<ProgramVariant>();

		
		
			// -Initial setup of directories----------
		idCounter = 1;
		ProgramVariant v_i = createProgramInstance(suspiciousList, idCounter);
		for(CtType type:mutatorSupporter.getFactory().Type().getAll()) {
			if(v_i.getBuiltClasses().get(type.getSimpleName())==null) {
				if(type instanceof CtClass) {
					v_i.getBuiltClasses().put(type.getSimpleName(), (CtClass)type);
				}
			}
		}
		variants.add(v_i);
		log.info("Creating program variant #" + idCounter + ", " + v_i.toString());

		if (ConfigurationProperties.getPropertyBool("saveall")) {
			String srcOutput = projectFacade.getInDirWithPrefix(v_i.currentMutatorIdentifier());
			mutatorSupporter.saveSourceCodeOnDiskProgramVariant(v_i, srcOutput);
		}

		

		return variants;
	}
	
	public ProgramVariant createProgramInstance(List suspiciousList, int idProgramInstance) {

		ProgramVariant progInstance = new ProgramVariant(idProgramInstance);
		for(CtType type:mutatorSupporter.getFactory().Type().getAll()) {
			if(progInstance.getBuiltClasses().get(type.getSimpleName())==null) {
				if(type instanceof CtType) {
					progInstance.getBuiltClasses().put(type.getSimpleName(), type);
				}
			}
		}
	

		log.debug("Creating variant " + idProgramInstance);
		ArrayList toDels = new ArrayList();
		if (!suspiciousList.isEmpty()) {
			for (Object obj : suspiciousList) {
				if(obj instanceof SuspiciousCode) {
					SuspiciousCode suspiciousCode = (SuspiciousCode)obj;
					List<SuspiciousModificationPoint> modifPoints = createModificationPoints(suspiciousCode, progInstance);
					if (modifPoints != null && !modifPoints.isEmpty()) {
						progInstance.addModificationPoints(modifPoints);					
					}else {
						toDels.add(obj);
					}
				}

			}
			log.info("Total suspicious from FL: " + suspiciousList.size() + ",  "
					+ progInstance.getModificationPoints().size());
			for(int i=0; i<suspiciousList.size(); i++) {
				log.info("  error:"+suspiciousList.get(i));
			}
		}
//		else {
//			// We do not have suspicious, so, we create modification for each
//			// statement
//
//			List<SuspiciousModificationPoint> pointsFromAllStatements = createModificationPoints(progInstance);
//			progInstance.getModificationPoints().addAll(pointsFromAllStatements);
//		}
		
		suspiciousList.removeAll(toDels);
		progInstance.setFitness(progInstance.getModificationPoints().size());
		log.info("Total ModPoint created: " + progInstance.getModificationPoints().size());
		int maxModPoints = ConfigurationProperties.getPropertyInt("maxmodificationpoints");
		if (progInstance.getModificationPoints().size() > maxModPoints) {
			progInstance.setModificationPoints(progInstance.getModificationPoints().subList(0, maxModPoints));
			log.info("Reducing Total ModPoint created to: " + progInstance.getModificationPoints().size());
		}

		// Defining identified of each modif point
		for (int i = 0; i < progInstance.getModificationPoints().size(); i++) {
			ModificationPoint mp = progInstance.getModificationPoints().get(i);
			mp.identified = i;
		}
		return progInstance;
	}
	
	public List<CtElement> retrieveCtElementForSuspectCode(SuspiciousCode candidate, CtElement ctclass)
			throws Exception {
		assert(candidate.getFileName().compareTo(ctclass.getPosition().getFile().getAbsolutePath())==0);
		CompilationErrorLocationPointerLauncher errorlocationlauncher = new CompilationErrorLocationPointerLauncher(MutationSupporter.getFactory());
		List<CtElement> susp = errorlocationlauncher.run(ctclass, (SuspiciousCompiledCode)candidate);
		List<CtElement> toDels = new ArrayList<CtElement>();
		for(CtElement element:susp) {
			try {
				element.toString();
			}catch(Exception e) {
				toDels.add(element);
			}
		}
		susp.removeAll(toDels);
		return susp;
	}


	
}
