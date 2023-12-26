package cn.sjtu.autoupdate.file;

import java.io.File;

import org.apache.log4j.Logger;

import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.manipulation.bytecode.OutputWritter;
import fr.inria.astor.core.manipulation.bytecode.compiler.ClassFileUtil;
import fr.inria.astor.core.manipulation.bytecode.entities.CompilationResult;
import fr.inria.astor.core.setup.ConfigurationProperties;
import spoon.compiler.Environment;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.support.JavaOutputProcessor;

public class AutoupdateOutputWritter{
	
	private Logger logger = Logger.getLogger(AutoupdateOutputWritter.class.getName());
	
	protected AutoupdateJavaOutputProcessor javaPrinter;
	protected Factory factory;
	public static final String CLASS_EXT = ".class";
	
	public AutoupdateOutputWritter(Factory factory) {
		this.factory = factory;
	}
	

	public void updateOutput(String output) {
		getEnvironment().setSourceOutputDirectory(new File(output));
		AutoupdateJavaOutputProcessor fileOutput = new AutoupdateJavaOutputProcessor(new AutoupdatePrettyPrinter(getEnvironment()));
		fileOutput.setFactory(getFactory());

		this.javaPrinter = fileOutput;
	}
	
	public void saveSourceCode(CtType element) {
		
		this.getEnvironment().setPreserveLineNumbers(ConfigurationProperties.getPropertyBool("preservelinenumbers"));
		if (javaPrinter == null) {
			throw new IllegalArgumentException("Java printer is null");
		}
		if (!element.isTopLevel()) {
			return;
		}
		// Create Java code and create ICompilationUnit
		try {
			javaPrinter.getCreatedFiles().clear();			
			javaPrinter.process(element);
		} catch (Exception e) {
			logger.error("Error saving ctclass " + element.getQualifiedName());
		}

	}
	
	public void saveByteCode(CompilationResult compilation, File outputDir) {
		try {
			outputDir.mkdirs();

			for (String compiledClassName : compilation.getByteCodes().keySet()) {
				String fileName = new String(compiledClassName).replace('.', File.separatorChar) + CLASS_EXT;
				byte[] compiledClass = compilation.getByteCodes().get(compiledClassName);
				ClassFileUtil.writeToDisk(true, outputDir.getAbsolutePath(), fileName, compiledClass);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public Environment getEnvironment() {
		return this.getFactory().getEnvironment();
	}

	/**
	 * Gets the associated factory.
	 */

	public Factory getFactory() {
		return this.factory;
	}

	public AutoupdateJavaOutputProcessor getJavaPrinter() {
		return javaPrinter;
	}

	public void setJavaPrinter(AutoupdateJavaOutputProcessor javaPrinter) {
		this.javaPrinter = javaPrinter;
	}

}
