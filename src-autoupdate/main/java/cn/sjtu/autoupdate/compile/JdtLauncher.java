package cn.sjtu.autoupdate.compile;

import static spoon.support.StandardEnvironment.DEFAULT_CODE_COMPLIANCE_LEVEL;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jdt.core.compiler.CategorizedProblem;

import com.martiansoftware.jsap.FlaggedOption;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.stringparsers.EnumeratedStringParser;
import com.martiansoftware.jsap.stringparsers.FileStringParser;

import cn.sjtu.autoupdate.engine.AutoupdateMutationSupporter;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousCode;
import fr.inria.astor.core.faultlocalization.entity.SuspiciousTestedCode;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectConfiguration;
import spoon.Launcher;
import spoon.OutputType;
import spoon.SpoonException;
import spoon.compiler.Environment;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.processing.Processor;
import spoon.reflect.CtModel;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtElement;
import spoon.reflect.declaration.CtType;
import spoon.reflect.factory.Factory;
import spoon.reflect.factory.FactoryImpl;
import spoon.reflect.visitor.DefaultJavaPrettyPrinter;
import spoon.reflect.visitor.Filter;
import spoon.reflect.visitor.PrettyPrinter;
import spoon.reflect.visitor.filter.AbstractFilter;
import spoon.support.DefaultCoreFactory;
import spoon.support.JavaOutputProcessor;
import spoon.support.StandardEnvironment;
import spoon.support.compiler.FileSystemFile;
import spoon.support.compiler.FileSystemFolder;
import spoon.support.compiler.VirtualFile;

import spoon.support.gui.SpoonModelTree;
//zhh
public class JdtLauncher {
	enum CLASSPATH_MODE {
		NOCLASSPATH, FULLCLASSPATH
	}

	protected FactoryImpl factory;

	private JdtBasedSpoonPartialCompiler jdtBuilder;


	
	public JdtLauncher(ProjectConfiguration properties) {
		factory =  (FactoryImpl) AutoupdateMutationSupporter.cleanFactory();
		getEnvironment()
		.setPreserveLineNumbers(ConfigurationProperties.getPropertyBool("preservelinenumbers"));
		getEnvironment().setComplianceLevel(ConfigurationProperties.getPropertyInt("javacompliancelevel"));
		getEnvironment().setShouldCompile(true);
		getEnvironment().setSourceClasspath(properties.getDependenciesString().split(File.pathSeparator));
		jdtBuilder = createCompiler();
	}

	public JdtBasedSpoonPartialCompiler createCompiler() {
		return createCompiler(factory);
	}
	
	public JdtBasedSpoonPartialCompiler createCompiler(Factory factory) {
		JdtBasedSpoonPartialCompiler comp = new JdtBasedSpoonPartialCompiler(factory);
		Environment env = getEnvironment();
		env.setAutoImports(true);
		// building
		comp.setBinaryOutputDirectory(new File(env.getBinaryOutputDirectory()));

		
		env.debugMessage("destination: " + comp.getBinaryOutputDirectory());
		env.debugMessage("source classpath: " + Arrays.toString(comp.getSourceClasspath()));
		env.debugMessage("template classpath: " + Arrays.toString(comp.getTemplateClasspath()));

		return comp;
	}
	
	public void addInputResource(String path) {
		File file = new File(path);
		if (file.isDirectory()) {
			addInputResource(new FileSystemFolder(file));
		} else {
			addInputResource(new FileSystemFile(file));
		}
	}

	/** adds a resource to be parsed to build the spoon model */
	public void addInputResource(SpoonResource resource) {
		jdtBuilder.addInputSource(resource);
	}

	public Environment getEnvironment() {
		return factory.getEnvironment();
	}
	/**
	 * A default logger to be used by Spoon.
	 */
	public static final Logger LOGGER = Logger.getLogger(Launcher.class);

	

	
	public Factory getFactory() {
		return factory;
	}

	

	public JavaOutputProcessor createOutputWriter() {
		JavaOutputProcessor outputProcessor = new JavaOutputProcessor(createPrettyPrinter());
		outputProcessor.setFactory(this.getFactory());
		return outputProcessor;
	}

	public PrettyPrinter createPrettyPrinter() {
		return new DefaultJavaPrettyPrinter(getEnvironment());
	}

	
	public static final IOFileFilter RESOURCES_FILE_FILTER = new IOFileFilter() {
		@Override
		public boolean accept(File file) {
			return !file.getName().endsWith(".java");
		}

		@Override
		public boolean accept(File file, String s) {
			return false;
		}
	};

	public static final IOFileFilter ALL_DIR_FILTER = new IOFileFilter() {
		@Override
		public boolean accept(File file) {
			return true;
		}

		@Override
		public boolean accept(File file, String s) {
			return false;
		}
	};

	
	public List<SuspiciousCompiledCode> compile() {
		long tstart = System.currentTimeMillis();
		jdtBuilder.build();
		getEnvironment().debugMessage("code parsed in " + (System.currentTimeMillis() - tstart));
		List<SuspiciousCompiledCode> suspList = jdtBuilder.getSuspCodeList();
		return suspList;
	}

	public List<SuspiciousCompiledCode> compileWithoutModel() {
		long tstart = System.currentTimeMillis();
		jdtBuilder.buildWithoutModel();
		getEnvironment().debugMessage("code parsed in " + (System.currentTimeMillis() - tstart));
		List<SuspiciousCompiledCode> suspList = jdtBuilder.getSuspCodeList();
		return suspList;
	}
	
	public JdtBasedSpoonPartialCompiler getModelBuilder() {
		return jdtBuilder;
	}

	
	public void setSourceOutputDirectory(String path) {
		setSourceOutputDirectory(new File(path));
	}

	
	public void setSourceOutputDirectory(File outputDirectory) {
		getEnvironment().setSourceOutputDirectory(outputDirectory);
		getEnvironment().setDefaultFileGenerator(createOutputWriter());
	}

	

		
}
