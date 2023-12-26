package fr.inria.astor.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import fr.inria.astor.core.entities.ProgramVariant;
import fr.inria.astor.core.manipulation.MutationSupporter;
import fr.inria.astor.core.setup.ConfigurationProperties;
import fr.inria.astor.core.setup.ProjectRepairFacade;
import spoon.reflect.declaration.CtType;

/**
 * Diff creator
 * 
 * @author Matias Martinez
 *
 */
public class PatchDiffCalculator {

	protected Logger log = Logger.getLogger(PatchDiffCalculator.class.getName());

	public final static String DIFF_SUFFIX = "_f";
	public static final String PATCH_DIFF_FILE_NAME = "patch.diff";

	/**
	 * Calculates the diff between a solution and the original program variant.
	 * 
	 * @param projectFacade
	 * @param programVariant
	 * @return
	 * @throws Exception
	 */
	public String getDiff(ProjectRepairFacade projectFacade, ProgramVariant originalVariant,
			ProgramVariant programVariant, boolean format, MutationSupporter mutsupporter) throws Exception {

		String diffResults = "";

		final String suffix = format ? DIFF_SUFFIX : "";

		// Get the path where the Default variant is located:
		String srcOutputfDefaultOriginal = projectFacade
				.getInDirWithPrefix(ProgramVariant.DEFAULT_ORIGINAL_VARIANT + suffix);

		ConfigurationProperties.setProperty("preservelinenumbers", Boolean.toString(!format));
		// save the default variant according to the format
		mutsupporter.saveSourceCodeOnDiskProgramVariant(originalVariant, srcOutputfDefaultOriginal);

		// get tge path of a Particular variant
		String srcOutputSolutionVariant = projectFacade
				.getInDirWithPrefix(programVariant.currentMutatorIdentifier() + suffix);

		for (CtType<?> t : programVariant.computeAffectedClassesByOperators()) {

			String fileName = t.getQualifiedName().replace(".", File.separator) + ".java";
			File foriginal = new File(srcOutputfDefaultOriginal + File.separator + fileName);
			File ffixed = new File(srcOutputSolutionVariant + File.separator + fileName);

			log.debug(foriginal.getAbsolutePath());
			log.debug(ffixed.getAbsolutePath());
			if (!foriginal.exists() || !ffixed.exists()) {
				log.error("A file with a solution does not exist");
				return null;
			}

			String diff = getDiff(foriginal, ffixed, fileName);
			diffResults += diff + '\n';
		}

		FileWriter patchWriter = new FileWriter(srcOutputSolutionVariant + File.separator + PATCH_DIFF_FILE_NAME);
		patchWriter.write(diffResults);
		patchWriter.flush();
		patchWriter.close();

		return diffResults;
	}

	public String getDiff(File original, File newvariant, String fileName) {

		try {
			ArrayList<String> command = new ArrayList<String>();
			command.add("C:\\Program Files (x86)\\GnuWin32\\bin\\diff.exe");
			command.add("-w");
			command.add("-b");
			command.add("-u");
			command.add("--label=" + fileName);
			command.add(original.getAbsolutePath());
			command.add(newvariant.getAbsolutePath());
			
			ProcessBuilder builder = new ProcessBuilder();
			builder.redirectErrorStream(true);
			builder.command(command);
			Process process = builder.start();
			
//			if(process.isAlive()){
//				process.waitFor();
//	        }

//				BufferedWriter p_stdin = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));

//				String command = "diff -w -b -u " + " --label=" + fileName + " --label=" + fileName + " "
//						+ original.getAbsolutePath() + " " + newvariant.getAbsolutePath();
//				log.debug("diff command : " + command);
//				p_stdin.write(command);
//				p_stdin.newLine();
//				p_stdin.flush();

				// end
//				p_stdin.write("exit");
//				p_stdin.newLine();
//				p_stdin.flush();


//			process.waitFor(30, TimeUnit.SECONDS);

			InputStream stderr = process.getErrorStream();
			InputStream stdout = process.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));

			BufferedReader readerE = new BufferedReader(new InputStreamReader(stderr));

			String out = readBuffer(reader);
			String outerror = readBuffer(readerE);
			if (!outerror.trim().isEmpty())
				log.error("Error reading diff: " + outerror);

			process.destroyForcibly();
			return out;
		} catch (Exception e) {
			e.printStackTrace();
			log.error(e);
		}
		return null;
	}

	private String readBuffer(BufferedReader reader) throws IOException {
		String line;
		String dd = "";
		while ((line = reader.readLine()) != null) {

			if (line.startsWith("---") || line.startsWith("+++"))
				dd += line.split("2017-")[0] + "\n";
			else
				dd += line + "\n";

		}
		return dd;
	}

}
