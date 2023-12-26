package cn.sjtu.autoupdate.engine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import org.apache.log4j.Logger;

import cn.sjtu.autoupdate.compile.JdtBasedSpoonPartialCompiler;
import cn.sjtu.autoupdate.file.AutoupdateOutputWritter;
import fr.inria.astor.core.entities.OperatorInstance;
import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.bytecode.OutputWritter;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectConfiguration;
import spoon.OutputType;
import spoon.SpoonModelBuilder.InputType;
import spoon.compiler.Environment;
import spoon.reflect.cu.CompilationUnit;
import spoon.reflect.cu.SourcePosition;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.CompilationUnitFactory;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.reflect.factory.TypeFactory;
import spoon.support.DefaultCoreFactory;
import spoon.support.StandardEnvironment;

public class AutoupdateMutationSupporter extends MutationSupporter {
	private Logger logger = Logger.getLogger(Thread.currentThread().getName());
	private ArrayList<File> savedFiles = new ArrayList<File>();
	protected AutoupdateOutputWritter output;
	
	public AutoupdateMutationSupporter() {
		factory = getFactory();
		this.output = new AutoupdateOutputWritter(factory);
	}
	@Override
	public void buildModel(String srcPathToBuild, String bytecodePathToBuild, String[] classpath) {
		buildCompileModel(srcPathToBuild, bytecodePathToBuild, classpath);
	}
	
	public static Factory cleanFactory() {
		factory = null;
		return getFactory();
	}
	
	public static Factory getFactory() {
		if (factory == null) {
			factory = createFactory();
//			factory.getEnvironment().setLevel("OFF");
//			factory.getEnvironment().setSelfChecks(true);

		}
		return factory;
	}

	private static Factory createFactory() {
//		Environment env = getEnvironment();
//		Factory factory = new FactoryImpl(new DefaultCoreFactory(), env);
		Environment env = getEnvironment();
		env.setPreserveLineNumbers(ConfigurationProperties.getPropertyBool("preservelinenumbers"));
		env.setComplianceLevel(ConfigurationProperties.getPropertyInt("javacompliancelevel"));
		env.setShouldCompile(true);
		env.setAutoImports(true);
		
		factory = new FactoryImpl(new DefaultCoreFactory(), env);
		return factory;
	}

	public void saveSourceCodeOnDiskProgramVariant(ProgramVariant instance, String srcOutput) throws Exception {
		this.output.updateOutput(srcOutput);
		Hashtable<String, CtType> classTable = new Hashtable<String, CtType>(); 
		for (CtType ctclass : instance.getBuiltClasses().values()) {
			classTable.put(ctclass.getQualifiedName(), ctclass);
		}
//		ProgramVariant parent = instance.getParent();
//		while(parent!=null) {
//			for (CtClass ctclass : parent.getBuiltClasses().values()) {
//				CtClass clazz = classTable.get(ctclass.getQualifiedName());
//				if(clazz==null) {
//					classTable.put(ctclass.getQualifiedName(), ctclass);
//				}
//			}
//			parent = parent.getParent();
//		}
		savedFiles.clear();
		for (CtType ctclass : classTable.values()) {			
			this.generateSourceCodeFromCtClass(ctclass);
			savedFiles.addAll(output.getJavaPrinter().getCreatedFiles());
		}
	
	}
	
	private void buildCompileModel(String srcPathToBuild, String bytecodePathToBuild, String[] classpath) {
		JdtBasedSpoonPartialCompiler jdtSpoonModelBuilder = null;
		logger.info("building model: " + srcPathToBuild + ", compliance level: "
				+ factory.getEnvironment().getComplianceLevel());
		factory.getEnvironment().setCommentEnabled(false);
		factory.getEnvironment().setNoClasspath(false);
		factory.getEnvironment().setPreserveLineNumbers(ConfigurationProperties.getPropertyBool("preservelinenumbers"));

		jdtSpoonModelBuilder = new JdtBasedSpoonPartialCompiler(factory);

		String[] sources = srcPathToBuild.split(File.pathSeparator);
		for (String src : sources) {
			if (!src.trim().isEmpty())
				jdtSpoonModelBuilder.addInputSource(new File(src));
		}
		logger.info("Classpath for building SpoonModel " + Arrays.toString(classpath));
		jdtSpoonModelBuilder.setSourceClasspath(classpath);

		jdtSpoonModelBuilder.build();

		if (ConfigurationProperties.getPropertyBool("savespoonmodelondisk")) {
			factory.getEnvironment().setSourceOutputDirectory(new File(srcPathToBuild));
			jdtSpoonModelBuilder.generateProcessedSourceFiles(OutputType.COMPILATION_UNITS);
			jdtSpoonModelBuilder.setBinaryOutputDirectory(new File(bytecodePathToBuild));
			jdtSpoonModelBuilder.compile(InputType.CTTYPES);
		}
	}
	
	public ArrayList<File> getSavedFiles() {
		return savedFiles;
	}
	public void generateSourceCodeFromCtClass(CtType<?> type) {
		// WorkArround, for cloned
		SourcePosition sp = type.getPosition();
		type.setPosition(null);

		if (output == null || output.getJavaPrinter() == null) {
			throw new IllegalArgumentException("Spoon compiler must be initialized");
		}
		output.saveSourceCode((CtType) type);
		type.setPosition(sp);
	}
	
}
