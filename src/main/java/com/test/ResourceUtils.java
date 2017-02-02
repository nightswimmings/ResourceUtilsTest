package com.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream.GetField;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public class ResourceUtils implements AutoCloseable {
	
	//Clean those operations were try close is not necessary as it is not jar??
	private URI baseURI;
	
	public static void main(String[] args) throws Exception{
		
		String existantInternalFile = "input/resourcee.csv";
		String nonExistantInternalFile = "input/resourcee4.csv";
		
		try {
			//Public methods Test
			System.out.println(new BufferedReader(new InputStreamReader(getProjectResourceInputStream(existantInternalFile))).readLine());
				
		} 
		catch (IOException | URISyntaxException e) {
			e.printStackTrace();
		}
	}
	
	private ResourceUtils() throws URISyntaxException, IOException{
		this(null);
	}
	
	private ResourceUtils(ClassLoader classLoader) throws URISyntaxException, IOException{
		//Todo: Not sure if we should contain URIException so it is not propagated up to the whole stack including public methods
		// That should be a internal logic error if the url returned is not valid, from the classloader, ignore it!!c
		if (null == classLoader) loadFileSystem(this.getClass().getClassLoader());
		else                     loadFileSystem(classLoader);
	}
	
	private void loadFileSystem(ClassLoader classLoader) throws URISyntaxException, IOException {
		//TODO: Smell #1 -> find a working version of "." for JAR
		// Perhaps using this very same lass package to find first folder
		baseURI = classLoader.getResource("com").toURI();
		try{ 
			Paths.get(baseURI).getFileSystem(); 
		} 
		catch(FileSystemNotFoundException e){
			FileSystems.newFileSystem(baseURI, Collections.emptyMap());	
			System.out.println("A new Filesystem for '"+baseURI.getScheme()+"' scheme is loaded.");
		}	
	}

	
	/** 
	 * Public Static Methods  (External Utils API)
	 **/
	
	//TOOD: Document:
	/* PRoj: Absolute to project, no leading slash or file glob wildcard */
	//See if writing to the very own running jar works in Linux as it is suggested here: http://stackoverflow.com/questions/25789729/writing-to-a-running-jar
	//If it does, documents windows exception	
	/* Ext: Absolute or relative */ //PILLAR Desc DEL FITXER Q MHE ENVIAT

	//Use copy aux method to MOVE, COPY and RENAME ops
	//TODO: LOG operations
	//TODO: PUBLICS WITH CLASSLOADER FOR THOSE with projects?
	//Todo: Execute validate in R/W functions??
	//Todo: Validate file is existing for read
	//Todo: Validate we are rading files not folders
	//Todo: Allow reading a whole folder by a high level function? by means of stdCopyOptions -> Atomic 
	//Todo: should/does copy/move work with folders? 
	//Todo: Do write methods what if we pass a folder -> NotAREgularFileException
	//TODO: what happens if read methods don't exist the file
	//TODO: Does it work on Win/Linux when write o perations on project and forom Jar?, document exception
	
	/* Read methods */
	
	public static BufferedReader readExternalResource(String srcExtRscPath) throws URISyntaxException, IOException{
		return new BufferedReader(new InputStreamReader(getExternalResourceInputStream(srcExtRscPath)));
	}
	
	public static BufferedReader readProjectResource(String srcProjRscPath) throws URISyntaxException, IOException, Exception{
		return new BufferedReader(new InputStreamReader(getProjectResourceInputStream(srcProjRscPath)));
	}

	public static InputStream getExternalResourceInputStream(String srcExtRscPath) throws URISyntaxException, IOException{
		ResourceUtils utils = new ResourceUtils(); 
		Path validatedExtRscPath = utils.resolveExternalResource(srcExtRscPath);
		return utils.readResourceStream(validatedExtRscPath);
	}
	//Creates a temporal external copy in case we are running from within jar
	//TODO optimize with a plain cl.getResourceAsStream() which does not deal with FS (if StackO question does not prosper
	public static InputStream getProjectResourceInputStream(String srcProjRscPath) throws URISyntaxException, IOException, Exception{
//By using plain old getResourceAsStream() -> url.getStream() [transparent regarding FS instance]
		try(ResourceUtils utils = new ResourceUtils()){ 
		Path validatedProjRscPath = utils.resolveProjectResource(srcProjRscPath);
		return utils.readResourceStream(validatedProjRscPath);
		}
//By using a temporal file copied from ZipFS
//		try(ResourceUtils utils = new ResourceUtils()){ 
//			Path validatedProjRscPath = utils.resolveProjectResourceJarAsTemp(srcProjRscPath);
//			return utils.readResourceStream(validatedProjRscPath);
//		}
	}
	
	//InMemory Reader
	public static List<String> getExternalResourceLines(String srcExtRscPath) throws URISyntaxException, IOException{
		ResourceUtils utils = new ResourceUtils(); 
		Path validatedExtRscPath = utils.resolveExternalResource(srcExtRscPath);
		return utils.loadResourceLines(validatedExtRscPath);
	}
	public static List<String> getProjectResourceLines(String srcProjRscPath) throws URISyntaxException, IOException, Exception{
		try(ResourceUtils utils = new ResourceUtils()){ 
			Path validatedProjRscPath = utils.resolveProjectResource(srcProjRscPath);
			return utils.loadResourceLines(validatedProjRscPath);
		}
	}
	
	public static <O> O processExternalResourceLines(String srcExtRscPath, Function<Stream<String>, O> lambda) throws URISyntaxException, IOException{
		ResourceUtils utils = new ResourceUtils(); 
		Path validatedExtRscPath = utils.resolveExternalResource(srcExtRscPath);
		return utils.processResourceLines(validatedExtRscPath, lambda);
	}
	public static <O> O processProjectResourceLines(String srcProjRscPath, Function<Stream<String>, O> lambda) throws Exception{
		try(ResourceUtils utils = new ResourceUtils()){ 
			Path validatedProjRscPath = utils.resolveProjectResource(srcProjRscPath);
			return utils.processResourceLines(validatedProjRscPath, lambda);
		}
	}
	
	/* Write methods */

	//FromMemory Writer
	public static void writeExternalResourceLines(String dstExtRscPath, List<String> lines, Boolean append) throws URISyntaxException, IOException{
		ResourceUtils utils = new ResourceUtils();
		Path validatedExtRscPath = utils.resolveExternalResource(dstExtRscPath);
		utils.writeResourceLines(lines, validatedExtRscPath, append);
	}
	
	public static void writeProjectResourceLines(String dstProjRscPath, List<String> lines, Boolean append) throws URISyntaxException, IOException{
		ResourceUtils utils = new ResourceUtils();
		Path validatedProjRscPath = utils.resolveProjectResource(dstProjRscPath);
		utils.writeResourceLines(lines, validatedProjRscPath, append);
	}
	
	/* Copy/Move/Rename methods */
	
	public static void externalCopy(String srcExtRscPath, String dstExtRscPath, 
									boolean delOriginal, boolean overwrite) throws IOException, URISyntaxException{
		ResourceUtils utils = new ResourceUtils();
		utils.copyResource(utils.resolveExternalResource(srcExtRscPath), 
						   utils.resolveExternalResource(dstExtRscPath),
						   delOriginal, overwrite);
	}
	public static void internalCopy(String srcProjRscPath, String dstProjRscPath, 
			                        boolean delOriginal, boolean overwrite) throws URISyntaxException, Exception{
		try( ResourceUtils utils = new ResourceUtils()){
			utils.copyResource(utils.resolveProjectResource(srcProjRscPath), 
					           utils.resolveProjectResource(dstProjRscPath),
					           delOriginal, overwrite);
		}
	} 
	public static void outwardsCopy(String srcProjRscPath, String dstExtRscPath, 
									boolean delOriginal, boolean overwrite) throws URISyntaxException, Exception{
		try( ResourceUtils utils = new ResourceUtils()){
			utils.copyResource(utils.resolveProjectResource(srcProjRscPath), 
			                   utils.resolveExternalResource(dstExtRscPath),
			                   delOriginal, overwrite);
		}
	}
	public static void inwardsCopy(String srcExtRscPath, String dstProjRscPath, 
						boolean delOriginal, boolean overwrite) throws URISyntaxException, IOException, Exception{
		try( ResourceUtils utils = new ResourceUtils()){
			utils.copyResource(utils.resolveExternalResource(srcExtRscPath), 
			utils.resolveProjectResource(dstProjRscPath),
			delOriginal, overwrite);
		}
	}

	/* Validate methods like exist or is readable is writable? */
	//TODO Validate public and private methods

	
	/**
	 * Private Aux Methods using I/O operations with non-resolving 
	 * logic (by means of Path(s)) 
	 **/
	
	/* Read methods */
		
	private InputStream readResourceStream(Path srcRscPath) throws IOException, URISyntaxException{
		//TODO: return srcRscPath.toUri().toURL().openStream();
		//Paths metaescapa el percentatge(i posa dos slash extres al principi po això no crec q afecti) amb lo qual no va si corregim això va sense dependre de FS
		return Files.newInputStream(srcRscPath);
	}
	
	private List<String> loadResourceLines(Path srcRscPath) throws IOException{
		return Files.readAllLines(srcRscPath);
	}
	
	//InMemory Mapper: http://nadeausoftware.com/articles/2008/02/java_tip_how_read_files_quickly
	/*private MappedByteBuffer loadResource(Path srcRscPath) throws IOException{
		//com.sun.nio.zipfs.ZipFileSystem cannot stream (map()) directly to memory so we need 
		//a temporal local FS copy either way if we are within jar.
		//IF a NotSupportedOperation were not thrown by ZipFS.map(), that would be called as
		//Channels.newChannel(System.out).write(loadResource(existantInternalFile)); throws UnsupportedException when inside Jar
		return FileChannel.open(srcRscPath, StandardOpenOption.READ).map(MapMode.READ_ONLY, 0L, Files.size(srcRscPath)); 
	}*/
	
	private <O> O processResourceLines(Path srcRscPath, Function<Stream<String>,O> linesStreamLambda) throws IOException{
		//TODO: Use parallelize?
		return linesStreamLambda.apply(Files.lines(srcRscPath));
	}
	
	/* Write methods */

	//If appendOrTruncate last boolean, representing overrideBehaviour, is omitted, then the writer throws an exception in case the file already exists
	private Path writeResourceLines(List<String> inMemoryLines, Path dstRscPath, Boolean appendOrTruncate) throws IOException{	
		Optional<Boolean> optional = Optional.ofNullable(appendOrTruncate);
		OpenOption overrideAllowed  = (optional.isPresent()  ? StandardOpenOption.CREATE : StandardOpenOption.CREATE_NEW);
		OpenOption overrideBehavior = (optional.orElse(true) ? StandardOpenOption.APPEND : StandardOpenOption.TRUNCATE_EXISTING);
		Files.createDirectories(dstRscPath.getParent());
		return Files.write(dstRscPath, inMemoryLines, StandardCharsets.UTF_8, overrideAllowed, overrideBehavior);
	}
	
	//TODO: ASESS
//	private BufferedWriter writeResource(Path dstRscPath, Boolean appendOrTruncate) throws IOException{	
//      Same as previous with a different return line.
//      But does it make sense? There's no way this works WITHIN JAR
//		return Files.newBufferedWriter(dstRscPath, StandardCharsets.UTF_8, overrideAllowed, overrideBehavior);
//	}

	/* Copy/Move/Rename methods */
	
	//Todo: createDirectories?
	private void copyResource(Path srcRscPath, Path dstRscPath, 
			                  boolean delOriginal, boolean overwrite) throws IOException{
		System.out.println("Moving from: "+srcRscPath.toUri()+" to "+dstRscPath.toUri());
		Files.createDirectories(dstRscPath.getParent());
		
		if (delOriginal) Files.move(srcRscPath, dstRscPath, (overwrite? StandardCopyOption.REPLACE_EXISTING : null));
		else 	         Files.copy(srcRscPath, dstRscPath, (overwrite? StandardCopyOption.REPLACE_EXISTING : null));			
	}
	
	/* Validation methods */
	
//	private boolean validateAccessToResource(Path rscPath){
//		if (Files.exists(rscPath)){
//			return Files.isReadable(rscPath);
//		}
//		else return Files.isWritable(rscPath);
//	}
	
	/* File Status */
	private Boolean exists(Path rscPath){
		if      (Files.exists(rscPath))    return true;
		else if (Files.notExists(rscPath)) return false; 
		return null;
	}
	
	
	//TODO: Works only with empty Folders
	//Returns false if file was not found.
	private boolean delete(Path rscPath) throws IOException{
		return Files.deleteIfExists(rscPath);
	}
	
	/** 
	 * Path Resolving Aux Methods
	 **/
	
	//TODO: option relative to Jar's directory (getProtectionDomain().getCodeSource().getLocation().getPath().getParent())
	
	private Path resolveExternalResource(String rscExtPath){
		return Paths.get(rscExtPath);
	}
	
	private Path resolveProjectResource(String rscProjPath){	
		return Paths.get(baseURI).getParent().resolve(rscProjPath);
	}

	private Path resolveProjectResourceJarAsTemp(String rscProjPath) throws IOException{
		if ("jar".equals(baseURI.getScheme())) {
			Path validatedRscPath = resolveProjectResource(rscProjPath);
			//TODO: StandardOpenOptions.DELETE_ON_CLOSE?
			Path tmpFile = Files.createTempFile("resource", ".tmp");
			copyResource(validatedRscPath, tmpFile, false, false);
			return tmpFile;
	 	}
		else return resolveProjectResource(rscProjPath);
	}
	
	/** 
	 * Clean non-default FileSystem if any (jar, etc.) 
	 **/ 
	@Override
	public void close() throws Exception {
		if (!"file".equals(baseURI.getScheme())) Paths.get(baseURI).getFileSystem().close();	
	}
}
