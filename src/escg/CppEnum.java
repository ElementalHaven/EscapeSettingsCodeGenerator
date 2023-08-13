package escg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CppEnum {
	static final Map<String, CppEnum>	BY_NAME	= new HashMap<>();
	
	static void writeAll(CppWriter writer) {
		boolean wroteSomething = false;
		for(CppEnum enm : BY_NAME.values()) {
			// put an empty line between each enum
			if(wroteSomething) writer.endLine();
			
			enm.createMaps(writer);
			wroteSomething = true;
		}
	}

	public String						name;
	// enum class Name {};
	public boolean						isClass;
	public String						namespace;
	public List<String>					values	= new ArrayList<>();

	public String fullyQualifiedName() {
		return namespace == null ? name : namespace + "::" + name;
	}
	
	public void createMaps(CppWriter writer) {
		final String fqName = fullyQualifiedName();
		
		// String to value ------------------------------------
		String mapName = "escgEnumS2V_" + name;
		
		// opening
		writer.startLine().append("static std::map<std::string, ").append(fqName);
		writer.append(">> ").append(mapName).append(" {").endLine();
		
		// values
		if(!values.isEmpty()) {
			writer.indent();
			String last = values.get(values.size() - 1);
			for(String value : values) {
				writer.startLine().append("{ ");
				writer.append('"').append(value.toLowerCase()).append("\", ");
				if(isClass) writer.append(fqName).append("::");
				writer.append(value).append(" }");
				if(value != last) writer.append(',');
				writer.endLine();
			}
			writer.unindent();
		}
		
		// closing
		writer.startLine().append("};").endLine();

		// Value to string -----------------------------------
		mapName = "escgEnumV2S_" + name;
		
		// opening
		writer.startLine().append("static std::map<").append(fqName);
		writer.append(", std::string>> ").append(mapName).append(" {").endLine();
		
		// values
		if(!values.isEmpty()) {
			writer.indent();
			String last = values.get(values.size() - 1);
			for(String value : values) {
				writer.startLine().append("{ ");
				if(isClass) writer.append(fqName).append("::");
				writer.append(value).append(", \"");
				writer.append(value.toLowerCase()).append(" }");
				if(value != last) writer.append(',');
				writer.endLine();
			}
			writer.unindent();
		}
		
		// closing
		writer.startLine().append("};").endLine();
	}
}