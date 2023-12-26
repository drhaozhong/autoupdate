package cn.sjtu.autoupdate.compile;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CategorizedProblem;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;

//import fr.inria.astor.approaches.autoupdate.engine.AutoupdateFactoryImpl;
import spoon.SpoonException;
import spoon.compiler.builder.JDTBuilder;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.declaration.CtImport;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.support.compiler.SpoonProgress;
import spoon.support.compiler.VirtualFolder;
import spoon.support.compiler.jdt.CompilationUnitFilter;
import spoon.support.compiler.jdt.JDTBasedSpoonCompiler;

import spoon.support.compiler.jdt.JDTTreeBuilder;

public class JdtBasedSpoonPartialCompiler extends JDTBasedSpoonCompiler {
	private List<SuspiciousCompiledCode> suspList = new ArrayList<SuspiciousCompiledCode>();
	
	public JdtBasedSpoonPartialCompiler(Factory factory) {
		super(factory);
		// TODO Auto-generated constructor stub
	}

	@Override
	public boolean build(JDTBuilder builder) {
		if (factory == null) {
			throw new SpoonException("Factory not initialized");
		}
		if (factory.getModel() != null && factory.getModel().isBuildModelFinished()) {			
			throw new SpoonException("Model already built");
		}
		
		boolean srcSuccess, templateSuccess;
		factory.getEnvironment().debugMessage("building sources: " + sources.getAllJavaFiles());
		long t = System.currentTimeMillis();
		javaCompliance = factory.getEnvironment().getComplianceLevel();
		srcSuccess = buildSources(builder);

//		reportProblems(factory.getEnvironment());

		factory.getEnvironment().debugMessage("built in " + (System.currentTimeMillis() - t) + " ms");
		factory.getEnvironment().debugMessage("building templates: " + templates.getAllJavaFiles());
		t = System.currentTimeMillis();
		templateSuccess = buildTemplates(builder);
		factory.getEnvironment().debugMessage("built in " + (System.currentTimeMillis() - t) + " ms");
//		checkModel();
		factory.getModel().setBuildModelIsFinished(true);
		return srcSuccess && templateSuccess;
	}
	
	
	public boolean buildWithoutModel() {
		if (factory == null) {
			throw new SpoonException("Factory not initialized");
		}
		if (factory.getModel() != null && factory.getModel().isBuildModelFinished()) {			
			throw new SpoonException("Model already built");
		}
		
		boolean srcSuccess;
		factory.getEnvironment().debugMessage("building sources: " + sources.getAllJavaFiles());
		long t = System.currentTimeMillis();
		javaCompliance = factory.getEnvironment().getComplianceLevel();
		srcSuccess = buildSources();

//		reportProblems(factory.getEnvironment());

		factory.getEnvironment().debugMessage("built in " + (System.currentTimeMillis() - t) + " ms");
	
		return srcSuccess;
		
	}

	private boolean buildSources() {
		CompilationUnitDeclaration[] units = buildUnits(null, sources, getSourceClasspath(), "");
		for(CompilationUnitDeclaration unit:units) {			
			String file1 = new String(unit.getFileName());
			ArrayList<CategorizedProblem> subProbs = new ArrayList<CategorizedProblem>(); 
			for(CategorizedProblem prob:probs) {
				String file2 = new String(prob.getOriginatingFileName());
				if(prob.isError()) {
					if(file1.compareTo(file2)==0) {
						subProbs.add(prob);
					}
				}
			}
			if(subProbs.size()>0) {
				MethodFinderVisitor visitor = new MethodFinderVisitor(subProbs, new String(unit.getFileName()));
				unit.traverse(visitor, unit.scope);
				List<SuspiciousCompiledCode> slist = visitor.getSuspCodeList();
				if(!slist.isEmpty()) {
					this.suspList.addAll(slist);
				}
			}
		}		
		return probs.size() == 0;
	}

	protected boolean buildSources(JDTBuilder jdtBuilder) {
		CompilationUnitDeclaration[] units = buildUnits(jdtBuilder, sources, getSourceClasspath(), "");
		for(CompilationUnitDeclaration unit:units) {			
			String file1 = new String(unit.getFileName());
			ArrayList<CategorizedProblem> subProbs = new ArrayList<CategorizedProblem>(); 
			for(CategorizedProblem prob:probs) {
				String file2 = new String(prob.getOriginatingFileName());
				if(prob.isError()) {
					if(file1.compareTo(file2)==0) {
						subProbs.add(prob);
					}
				}
			}
			if(subProbs.size()>0) {
				MethodFinderVisitor visitor = new MethodFinderVisitor(subProbs, new String(unit.getFileName()));
				unit.traverse(visitor, unit.scope);
				List<SuspiciousCompiledCode> slist = visitor.getSuspCodeList();
				if(!slist.isEmpty()) {
					this.suspList.addAll(slist);
				}
			}
		}
		buildModel(units);
		return probs.size() == 0;
	}

	public List<SuspiciousCompiledCode> getSuspCodeList() {
		return suspList;
	}
	
	protected void buildModel(CompilationUnitDeclaration[] units) {
		if (getEnvironment().getSpoonProgress() != null) {
			getEnvironment().getSpoonProgress().start(SpoonProgress.Process.MODEL);
		}
		JdtTreeBuilder builder = new JdtTreeBuilder(factory);
		List<CompilationUnitDeclaration> unitList = this.sortCompilationUnits(units);

		int i = 0;
		unitLoop:
		for (CompilationUnitDeclaration unit : unitList) {
			if (unit.isModuleInfo() || !unit.isEmpty()) {
				final String unitPath = new String(unit.getFileName());
				for (final CompilationUnitFilter cuf : compilationUnitFilters) {
					if (cuf.exclude(unitPath)) {
						// do not traverse this unit
						continue unitLoop;
					}
				}
				unit.traverse(builder, unit.scope);
				if (getFactory().getEnvironment().isCommentsEnabled()) {
					new JdtCommentBuilder(unit, factory).build();
				}
				if (getEnvironment().getSpoonProgress() != null) {
					getEnvironment().getSpoonProgress().step(SpoonProgress.Process.MODEL, new String(unit.getFileName()), ++i, unitList.size());
				}
			}
			// we need first to go through the whole model before getting the right reference for imports
			if (getFactory().getEnvironment().isAutoImports()) {
				if (getEnvironment().getSpoonProgress() != null) {
					getEnvironment().getSpoonProgress().start(SpoonProgress.Process.IMPORT);
				}
				new JdtImportBuilder(unit, factory).build();
				if (getEnvironment().getSpoonProgress() != null) {
					getEnvironment().getSpoonProgress().step(SpoonProgress.Process.IMPORT, new String(unit.getFileName()), ++i, units.length);
				}
				if (getEnvironment().getSpoonProgress() != null) {
					getEnvironment().getSpoonProgress().end(SpoonProgress.Process.IMPORT);
				}
			}

		}
		if (getEnvironment().getSpoonProgress() != null) {
			getEnvironment().getSpoonProgress().end(SpoonProgress.Process.MODEL);
		}

		
	}

	

}
