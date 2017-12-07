package it.bancaditalia.oss.plugins;

import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.DirectoryScanner;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.XML;

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

	public static enum Field
	{
		name, size, creationTime, lastModifiedTime 
	}
	
	/**
	 * The Maven project.
	 * 
	 * @parameter property="${project}"
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
	public Field[] fields = { Field.name };

	/**
	 * xml or json (default)
	 * 
	 * @parameter default-value="json"
	 */
	public String type;

	/**
	 * Whether to ignore case
	 * 
	 * @parameter default-value=false
	 */
	public boolean caseSensitive;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {

		if ("xml".equals(type))
			this.outputFile = this.outputFile.replace(".json", ".xml");

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

		JSONArray array = new JSONArray();
		FileSystem fileSystem = FileSystems.getDefault();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
		dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		Set<String> unrecognizedFields = new HashSet<>();
		
		try {
			Files.createDirectories(Paths.get(this.outputFile).getParent());
		} catch (IOException e) {
			throw new MojoExecutionException("Could not create build directory", e);
		}

		try (FileWriter fileWriter = new FileWriter(this.outputFile)) {

			String[] includedFiles = scanner.getIncludedFiles();

			for (String fileName : includedFiles) {
				Path file = fileSystem.getPath(baseDir, fileName);
				JSONObject element = new JSONObject();
				for (Field field : fields)
					switch (field) {
					case name:
						element.put(field.toString(), fileName);
						break;
					case size:
						element.put(field.toString(), (Long) Files.getAttribute(file, "basic:size"));
						break;
					case creationTime:
						element.put(field.toString(), dateFormat.format(
								new Date(((FileTime) Files.getAttribute(file, "basic:creationTime")).toMillis())));
						break;
					case lastModifiedTime:
						element.put(field.toString(), dateFormat.format(
								new Date(((FileTime) Files.getAttribute(file, "basic:lastModifiedTime")).toMillis())));
						break;
					default:
						if (unrecognizedFields.add(field.toString()))
							log.warn("Field name \"" + field + "\" not recognized.");
					}
				array.put(element);
			}

			log.info("File list contains " + array.length() + " files");

			if ("json".equals(this.type)) {
				fileWriter.write(array.toString(4));
			} else if ("xml".equals(this.type)) {
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
				StreamSource source = new StreamSource(new StringReader("<files>" + XML.toString(array, "file") + "</files>"));
				transformer.transform(source, new StreamResult(fileWriter));
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
		} catch (IOException | TransformerFactoryConfigurationError | TransformerException ex) {
			throw new MojoExecutionException("Could not write output file", ex);
		}
	}
}
