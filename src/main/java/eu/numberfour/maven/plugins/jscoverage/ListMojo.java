package eu.numberfour.maven.plugins.jscoverage;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * <p>
 * Creates a JSON formatted list of files form a base directory, and optional
 * includes and excludes.
 * </p>
 * 
 * @author <a href="mailto:leonard.ehrenfried@web.de">Leonard Ehrenfried</a>
 * 
 * @goal list
 * @requiresDependencyResolution compile
 * @description Creates a JSON-formatted list of files
 */
public class ListMojo extends AbstractMojo {

	public static final String NEW_LINE = System.getProperty("line.separator");
	public static final String FILE_SEPARATOR = System.getProperty("file.separator");

	/**
	 * The Maven project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
	private MavenProject project;
	/**
	 * File into which to save the output of the transformation.
	 * 
	 * @parameter default-value="${basedir}/target/file-list.json"
	 */
	private String outputFile;
	/**
	 * Base directory of the scanning process
	 * 
	 * @parameter default-value="${basedir}/target/"
	 */
	public String baseDir;
	/**
	 * Ant-style include pattern.
	 * 
	 * For example **.* is all files
	 * 
	 * @parameter
	 */
	public String[] includes;
	/**
	 * Ant-style exclude pattern.
	 * 
	 * For example **.* is all files
	 * 
	 * @parameter
	 */
	public String[] excludes;

	/**
	 * Fields to print.
	 * 
	 * Allowed fields: name, size, creationTime, lastModifiedFile
	 * 
	 * @parameter
	 */
	public String[] fields = { "name" };

	/**
	 * @parameter default-value="json"
	 */
	public String type;

	/**
	 * Whether to ignore case
	 * 
	 * @parameter
	 */
	public boolean caseSensitive;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		Log log = getLog();
		log.info("");
		log.info("Creating file list ");
		log.info("Basedir:  " + this.baseDir);
		log.info("Output:   " + this.outputFile);
		log.info("Includes: " + Arrays.toString(this.includes));
		log.info("Exludes:  " + Arrays.toString(this.excludes));
		log.info("Type:  " + this.type);

		DirectoryScanner scanner = new DirectoryScanner();
		scanner.setBasedir(this.baseDir);
		scanner.setIncludes(this.includes);
		scanner.setExcludes(this.excludes);
		scanner.setCaseSensitive(this.caseSensitive);
		scanner.scan();

		JsonArray array = new JsonArray();
		FileSystem fileSystem = FileSystems.getDefault();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

		try (FileWriter fileWriter = new FileWriter(this.outputFile)) {

			String[] includedFiles = scanner.getIncludedFiles();

			for (String fileName : includedFiles) {
				Path file = fileSystem.getPath(baseDir, fileName);
				JsonObject element = new JsonObject();
				for (String field : fields)
					switch (field) {
					case "name":
						element.addProperty(field, fileName);
						break;
					case "size":
						element.addProperty(field, (Long) Files.getAttribute(file, "basic:size"));
						break;
					case "creationTime":
						element.addProperty(field, dateFormat.format(
								new Date(((FileTime) Files.getAttribute(file, "basic:creationTime")).toMillis())));
						break;
					case "lastModifiedTime":
						element.addProperty(field, dateFormat.format(
								new Date(((FileTime) Files.getAttribute(file, "basic:lastModifiedTime")).toMillis())));
						break;
					}
				array.add(element);
			}

			log.info("File list contains " + array.size() + " files");

			if ("json".equals(this.type)) {
				fileWriter.write(new GsonBuilder().setPrettyPrinting().create().toJson(array));
			} else if ("junit".equals(this.type)) {
				StringBuilder suiteContent = new StringBuilder();
				suiteContent.append("package n4.quat.selenium.acceptancetest.suites;");
				suiteContent.append(NEW_LINE);
				suiteContent.append(NEW_LINE);
				suiteContent.append("import org.junit.runner.RunWith;");
				suiteContent.append(NEW_LINE);
				suiteContent.append("import org.junit.runners.Suite.SuiteClasses;");
				suiteContent.append(NEW_LINE);
				suiteContent.append(NEW_LINE);
				suiteContent.append("@RunWith(org.junit.runners.Suite.class)");
				suiteContent.append(NEW_LINE);
				suiteContent.append("@SuiteClasses( {");
				suiteContent.append(NEW_LINE);
				boolean firstTime = true;
				for (String string : includedFiles) {
					suiteContent.append("\t");
					if (firstTime) {
						firstTime = false;
					} else {
						suiteContent.append(",");
					}
					String pattern = FILE_SEPARATOR;
					if ("\\".equals(FILE_SEPARATOR)) {
						// we have to quote the back slash
						pattern = "\\" + FILE_SEPARATOR;
					}
					suiteContent.append(string.replaceAll(pattern, ".").replaceAll("java", "class"));
					suiteContent.append(NEW_LINE);
				}
				suiteContent.append("} )");
				suiteContent.append(NEW_LINE);
				suiteContent.append("public class AllTestsSuite { }");
				suiteContent.append(NEW_LINE);
				fileWriter.write(suiteContent.toString());
			}
		} catch (IOException ex) {
			throw new MojoFailureException("Could not write output file.");
		}
	}
}
