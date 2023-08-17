package escg;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class IOGrouping {
	/** Types that will be guaranteed to be present, including in the header */
	public final List<String>	mainTypes		= new ArrayList<>();
	public final List<File>		inputFiles		= new ArrayList<>();
	public String				outputName		= "escg";
	/**
	 * The main types and types they are reliant on.<br>
	 * After a certain point in the program, this list will be sorted
	 * roughly in order of dependencies(least to most). */
	public final List<String>	typesToExport	= new ArrayList<>();
	public final List<String>	enumsToExport	= new ArrayList<>();
	/**
	 * Basically inputFiles, but each time one is specified
	 * as imported in a property, it's removed from this list.<br>
	 * All files left in this list are then relativized and included in the header 
	 */
	public final List<String>	unimportedFiles	= new ArrayList<>();
	public File					outputHeader;
	public File					outputFile;
	public File					folder;
	
	CppWriter					writer			= new CppWriter();
	
	public boolean preliminaryValidation() {
		if(inputFiles.isEmpty()) {
			System.err.println("No input C++ files to process!");
			return false;
		}
		Iterator<File> iter = inputFiles.iterator();
		while(iter.hasNext()) {
			File f = iter.next();
			if(!f.exists() || !f.canRead()) {
				System.err.println('"' + f.getPath() + 
						"\" is not an existing readable file");
				iter.remove();
			}
		}
		if(inputFiles.isEmpty()) {
			System.err.println("None of the files specified were readable. " + 
					"Can not continue");
			return false;
		}

		folder = inputFiles.get(0).getParentFile();
		Path dir = folder.toPath();
		
		// add potential imports in case the user doesn't manually specify them
		for(File f : inputFiles) {
			String path = dir.relativize(f.toPath()).toString();
			// No Windows paths. Period.
			path.replace('\\', '/');
			unimportedFiles.add('"' + path + '"');
		}
		
		return true;
	}
	
	public boolean lateValidation() {
		// done now instead of the end of preliminaryValidation
		// so that imported files can specify output name
		outputHeader = new File(folder, outputName + ".h");
		outputFile = new File(folder, outputName + ".cpp");
		
		// first check all the main types exist and collect their dependencies
		for(String type : mainTypes) {
			Group main = Group.BY_NAME.get(type);
			if(main == null) {
				System.err.println("No C++ file parsed contained the main type \"" + type + '"');
				return false;
			}
			if(!typesToExport.contains(type)) {
				typesToExport.add(type);
			}
			main.getDependencies(typesToExport, enumsToExport);
		}
		
		// then check all the dependency types exist
		for(String type : typesToExport) {
			Group group = Group.BY_NAME.get(type);
			if(group == null) {
				System.err.println("No C++ file parsed contained the dependent type \"" + type + '"');
				return false;
			}
		}
		
		// enums are not checked for their existence
		// as we wouldn't know they're enums if they didn't exist
		// and they would just be assumed to by dependency types
		
		// sort the types so we don't have to have forward declarations for methods
		// we still have to sort even if we included the imported headers
		// because dependencies don't show up in said headers
		List<String> sortedTypes = new ArrayList<>(typesToExport.size());
		List<String> localDependencies = new ArrayList<>();
		while(!typesToExport.isEmpty()) {
			Iterator<String> iter = typesToExport.iterator();
			while(iter.hasNext()) {
				localDependencies.clear();
				String typeName = iter.next();
				Group type = Group.BY_NAME.get(typeName);
				// we only need to check structs & classes, not enums
				type.getDependencies(localDependencies, null);
				if(sortedTypes.containsAll(localDependencies)) {
					sortedTypes.add(typeName);
					iter.remove();
				}
			}
		}
		typesToExport.addAll(sortedTypes);
		
		return true;
	}
}