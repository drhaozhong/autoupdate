package cn.sjtu.autoupdate.engine;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import com.martiansoftware.jsap.JSAPException;

import cn.sjtu.autoupdate.compile.JdtLauncher;
import cn.sjtu.autoupdate.compile.SuspiciousCompiledCode;
import fr.inria.astor.core.entities.ModificationPoint;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.entities.SuspiciousModificationPoint;
import fr.inria.astor.core.entities.VariantValidationResult;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.manipulation.filters.TargetElementProcessor;
import fr.inria.astor.core.manipulation.sourcecode.BlockReificationScanner;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectConfiguration;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import fr.inria.astor.core.solutionsearch.EvolutionarySearchEngine;
import fr.inria.astor.core.solutionsearch.population.ProgramVariantFactory;
import fr.inria.astor.core.stats.Stats;
import fr.inria.astor.core.stats.Stats.GeneralStatEnum;
import fr.inria.main.AstorOutputStatus;
import fr.inria.main.evolution.ExtensionPoints;

import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;


/**
 * Engine for Zimin approach which validates variants stored on Files.
 * 
 * @author Matias Martinez
 *
 */
public class AutoupdateEngine extends EvolutionarySearchEngine {
	public AutoupdateEngine(MutationSupporter mutatorExecutor, ProjectRepairFacade projFacade) throws JSAPException {
		super(mutatorExecutor, projFacade);
		this.setMutatorSupporter(new AutoupdateMutationSupporter());
		
	}
	
	public RepairResult startEvolution() throws Exception {

		log.info("\n----Starting Solution Search");

		generationsExecuted = 0;
		nrGenerationWithoutModificatedVariant = 0;
		boolean stopSearch = false;

		dateInitEvolution = new Date();

		int maxMinutes = ConfigurationProperties.getPropertyInt("maxtime");

		RepairResult rr = null;
		while (!stopSearch) {

			if (!(generationsExecuted < ConfigurationProperties.getPropertyInt("maxGeneration"))) {
				log.debug("\n Max generation reached " + generationsExecuted);
				this.outputStatus = AstorOutputStatus.MAX_GENERATION;
				break;
			}

			if (!(belowMaxTime(dateInitEvolution, maxMinutes))) {
				log.debug("\n Max time reached " + generationsExecuted);
				this.outputStatus = AstorOutputStatus.TIME_OUT;
				break;
			}

			generationsExecuted++;
			log.debug("\n----------Running generation: " + generationsExecuted + ", population size: "
					+ this.variants.size());
			try {
				RepairResult result = processNextGenerations(generationsExecuted);
				if(result!=null) {
					if(rr==null) {
						rr = result;
					}else if(result.getFitness()<rr.getFitness()) {
						rr = result;
					}
				}
				if (result!=null) {
					if(result.getFitness() == 0) {
						stopSearch =
								// one solution
								(ConfigurationProperties.getPropertyBool("stopfirst")
										// or nr solutions are greater than max allowed
										|| (this.solutions.size() >= ConfigurationProperties
												.getPropertyInt("maxnumbersolutions")));
		
						if (stopSearch) {
							log.debug("\n Max Solution found " + this.solutions.size());
							this.outputStatus = AstorOutputStatus.STOP_BY_PATCH_FOUND;
						}
					}
				}
			} catch (Throwable e) {
				log.error("Error at generation " + generationsExecuted + "\n" + e);
				e.printStackTrace();
				this.outputStatus = AstorOutputStatus.ERROR;
				break;
			}

			if (this.nrGenerationWithoutModificatedVariant >= ConfigurationProperties
					.getPropertyInt("nomodificationconvergence")) {
				log.error(String.format("Stopping main loop at %d generation", generationsExecuted));
				this.outputStatus = AstorOutputStatus.CONVERGED;
				break;
			}
		}
		return rr;
	}
	
	protected RepairResult processNextGenerations(int generation)  {

		log.debug("\n***** Generation " + generation + " : " + this.nrGenerationWithoutModificatedVariant);
		boolean foundSolution = false;

		List<ProgramVariant> temporalInstances = new ArrayList<ProgramVariant>();
		

		currentStat.increment(GeneralStatEnum.NR_GENERATIONS);
		
		RepairResult bestRepair = null;
		
		for (ProgramVariant parentVariant : variants) {
			temporalInstances.add(parentVariant);
			log.debug("**Parent Variant: " + parentVariant);
			try {
				ProgramVariant newVariant = createNewProgramVariant(parentVariant, generation);
	
				if (newVariant == null) {
					continue;
				}
				List<SuspiciousCompiledCode> tmpSuspicousList = compileCreatedVariant(newVariant, generation);
				
				ProgramVariant newCopyVariant = variantFactory.createProgramInstance(tmpSuspicousList, newVariant.getId());
				newCopyVariant.setParent(newVariant.getParent());
				temporalInstances.add(newCopyVariant);
				
				if(bestRepair==null) {
					bestRepair = new RepairResult();
					bestRepair.setResult(tmpSuspicousList, newVariant.getId());
				}else if(newCopyVariant.getFitness()<bestRepair.getFitness()) {
					bestRepair.setResult(tmpSuspicousList, newVariant.getId());
				}
				
				if (newCopyVariant.getFitness()==0) {
					foundSolution = true;
					newCopyVariant.setBornDate(new Date());
				}
				
				// Finally, reverse the changes done by the child
				reverseOperationInModel(newVariant, generation);
				this.validateReversedOriginalVariant(newVariant);
				if (foundSolution && ConfigurationProperties.getPropertyBool("stopfirst")) {
					break;
				}
			}catch(Exception e) {
				e.printStackTrace();
//				System.err.println("Fail to generate new variants.");
//				System.err.println("Fail to generate new variants.");
				continue;
			}
		}
		
		int oldSize = variants.size();
		prepareNextGeneration(temporalInstances, generation);
		int newSize = variants.size();
		log.info("Variant delta is "+(newSize-oldSize));
		if(newSize-oldSize==0) {
			this.nrGenerationWithoutModificatedVariant++;
		}else {
			this.nrGenerationWithoutModificatedVariant = 0;
		}
		

		return bestRepair;
	}
	
	

	



	


	public List<SuspiciousCompiledCode> compileCreatedVariant(ProgramVariant programVariant, int generation) throws Exception {
		saveVariant(programVariant);
		ArrayList<File> savedFiles = ((AutoupdateMutationSupporter)mutatorSupporter).getSavedFiles();
		
		JdtLauncher launcher = new JdtLauncher(this.projectFacade.getProperties());
		ArrayList<String> modifiedFiles = new ArrayList<String>(); 
		for(File path:savedFiles) {
			log.debug("Add folder to compile: " + path);
			launcher.addInputResource(path.getAbsolutePath());
			modifiedFiles.add(path.getName());
		}
		

		log.info("Compiling original code from " + launcher.getModelBuilder().getInputSources()
				+ "\n bytecode saved in " + launcher.getModelBuilder().getBinaryOutputDirectory());		
	
		List<SuspiciousCompiledCode> suspicousCodeList = launcher.compile();
		
		
		URL[] originalURL = projectFacade.getClassPathURLforProgramVariant(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);

		CompilationResult compilation = compiler.compile(programVariant, originalURL);

		boolean childCompiles = compilation.compiles();
		programVariant.setCompilation(compilation);

		storeModifiedModel(programVariant);

		if (ConfigurationProperties.getPropertyBool("saveall")) {
			this.saveVariant(programVariant);
		}
		

		log.debug("-The child compiles: id " + programVariant.getId());
		currentStat.increment(GeneralStatEnum.NR_RIGHT_COMPILATIONS);

		
		double fitness = suspicousCodeList.size();
		programVariant.setFitness(fitness);
		if(fitness==0) {
			programVariant.setIsSolution(true);
		}
		
		log.debug("-fitness " + programVariant.getFitness());
		saveStaticSucessful(programVariant.getId(), generation);
		
		programVariant.setCompilation(compilation);

	
		return suspicousCodeList;
		

		/*URL[] originalURL = projectFacade.getClassPathURLforProgramVariant(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);

		CompilationResult compilation = compiler.compile(programVariant, originalURL);

		boolean childCompiles = compilation.compiles();
		programVariant.setCompilation(compilation);

		storeModifiedModel(programVariant);

		if (ConfigurationProperties.getPropertyBool("saveall")) {
			this.saveVariant(programVariant);
		}

		
		log.debug("-The child compiles: id " + programVariant.getId());
		currentStat.increment(GeneralStatEnum.NR_RIGHT_COMPILATIONS);

		
		double fitness = compilation.getErrorList().size();
		programVariant.setFitness(fitness);
		if(fitness==0) {
			programVariant.setIsSolution(true);
		}

		
		log.info("-Found Solution, child variant #" + programVariant.getId());
		saveStaticSucessful(programVariant.getId(), generation);
		saveVariant(programVariant);
		
		
		// In case that the variant a) does not compile; b) compiles but it's
		// not adequate
		Stats.currentStat.getIngredientsStats().storeIngCounterFromFailingPatch(programVariant.getId());
		return programVariant.isSolution();*/
	}
	
	
	
	
	public void saveVariant(ProgramVariant programVariant) throws Exception {
		savePatchDiff(programVariant, true);
	}

	@Override
	protected void loadTargetElements() throws Exception {
		ExtensionPoints extensionPointpoint = ExtensionPoints.TARGET_CODE_PROCESSOR;

		List<TargetElementProcessor<?>> loadedTargetElementProcessors = loadTargetElements(extensionPointpoint);
		
		this.setTargetElementProcessors(loadedTargetElementProcessors);
		this.setVariantFactory(new AutoupdateVariantFactory());		
	}

	

	@Override
	public List calculateSuspicious() throws Exception {
		ProjectConfiguration properties = projectFacade.getProperties();
		String path_src = properties.getWorkingDirForSource();
		return getCompuileErrors(path_src);
	}

	private List<SuspiciousCompiledCode> getCompuileErrors(String path) {
		log.debug("Add folder to compile: " + path);
		JdtLauncher launcher = new JdtLauncher(projectFacade.getProperties());
		launcher.addInputResource(path);
	

		log.info("Compiling original code from " + launcher.getModelBuilder().getInputSources()
				+ "\n bytecode saved in " + launcher.getModelBuilder().getBinaryOutputDirectory());		
	
		return launcher.compile();			
	}
	
	public List<SuspiciousModificationPoint> getSuspicious() {
		List<SuspiciousModificationPoint> ls = new ArrayList<>();
		ProgramVariant pv = this.getVariants().get(0);
		for (ModificationPoint modificationPoint : pv.getModificationPoints()) {
			if (modificationPoint instanceof SuspiciousModificationPoint) {
				ls.add((SuspiciousModificationPoint) modificationPoint);
			}

		}
		return ls;
	}

	public void initModel() throws Exception {
		if (!MutationSupporter.getFactory().Type().getAll().isEmpty()) {
			if (ConfigurationProperties.getPropertyBool("resetmodel")) {
				Factory fcurrent = AutoupdateMutationSupporter.getFactory();
				log.debug("The Spoon Model was already built.");
				Factory fnew = AutoupdateMutationSupporter.cleanFactory();
				log.debug("New factory created? " + !fnew.equals(fcurrent));
			} else {
				log.debug("we keep previous factory");
				// we do not generate a new model
				return;
			}
		}
	
		String codeLocation = "";
		if (ConfigurationProperties.getPropertyBool("parsesourcefromoriginal")) {
			List<String> codeLocations = projectFacade.getProperties().getOriginalDirSrc();
			for (String source : codeLocations) {
				codeLocation += source + File.pathSeparator;
			}
			if (codeLocation.length() > 0) {
				codeLocation = codeLocation.substring(0, codeLocation.length() - 1);
			}
		} else {
			codeLocation = projectFacade.getInDirWithPrefix(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);
		}

		String bytecodeLocation = projectFacade.getOutDirWithPrefix(ProgramVariant.DEFAULT_ORIGINAL_VARIANT);
		String classpath = projectFacade.getProperties().getDependenciesString();
		String[] cpArray = classpath.split(File.pathSeparator);

		log.info("Creating model,  Code location from working folder: " + codeLocation);

		try {			
			mutatorSupporter.buildModel(codeLocation, bytecodeLocation, cpArray);
			log.debug("Spoon Model built from location: " + codeLocation);
		} catch (Exception e) {
			log.error("Problem compiling the model with compliance level "
					+ ConfigurationProperties.getPropertyInt("javacompliancelevel"));
			log.error(e.getMessage());
			throw e;
		}

		///// ONCE ASTOR HAS BUILT THE MODEL,
		///// We apply different processes and manipulation over it.

		// We process the model to add blocks as parent of statement which are
		// not contained in a block
		BlockReificationScanner visitor = new BlockReificationScanner();
		for (CtType c : mutatorSupporter.getFactory().Type().getAll()) {
			c.accept(visitor);
		}

	}

}
