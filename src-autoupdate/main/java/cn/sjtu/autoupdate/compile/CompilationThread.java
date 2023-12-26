package cn.sjtu.autoupdate.compile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.eclipse.jdt.core.compiler.CategorizedProblem;

import fr.inria.astor.core.setup.ProjectConfiguration;

public class CompilationThread implements Callable<ArrayList<CategorizedProblem>>{
	private String libPath;
	private ArrayList<File> sourceDirs;
	
	public CompilationThread(String libPath, ArrayList<File> sources) {
		this.libPath = libPath;
		this.sourceDirs = sources;
	}

	@Override
	public ArrayList<CategorizedProblem> call() throws Exception {
	ProjectConfiguration properties = new ProjectConfiguration();
		properties.setDependencies(libPath);
		
		JdtLauncher launcher = new JdtLauncher(properties);
		for(File dir:sourceDirs) {
			launcher.addInputResource(dir.getAbsolutePath());
		}
		List<SuspiciousCompiledCode> errorlist = launcher.compileWithoutModel();
		ArrayList<CategorizedProblem> errors = new ArrayList<CategorizedProblem>();
		for(SuspiciousCompiledCode error:errorlist) {
			errors.add(error.getProblem());
		}
		return errors;
	}

}
