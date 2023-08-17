package escg;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Group extends SettingOrGroup {
	static final Map<String, Group>	BY_NAME			= new HashMap<>();

	public List<SettingOrGroup>		items			= new ArrayList<>();
	public String					namespace;
	/**
	 * If applicable(aka, not an anonymous struct) The name that will be used in
	 * the struct map and in methods names operating on these
	 */
	public String					typeName;
	public List<Group>				parentTypes		= new ArrayList<>();

	// used by the parser to know whether the current item
	// should be added to the struct or not.
	// items need to be public for escg code to acces them
	public transient boolean		currentlyPublic	= true;

	// creates a clone of this group
	// mainly exists for supporting external and subclassed structs/classes
	@Override
	public Group clone() {
		Group group = new Group();
		for(SettingOrGroup item : items) {
			if(item.getClass() == Group.class) {
				item = ((Group) item).clone();
			}
			// it should be safe to add "primitive" settings without cloning
			group.items.add(item);
		}
		return group;
	}
	
	public void writerReaderCall(CppWriter writer, String jsonVarName, String structName) {
		if(typeName != null) {
			writer.startLine().append("escgRead_").append(typeName);
			writer.append('(').append(jsonVarName).append(", ");
			writer.append(structName).append(");").endLine();
		} else {
			writeReaderCode(writer, jsonVarName, structName);
		}
	}
	
	public void writeReaderMethod(CppWriter writer) {
		writer.startLine().append("void escgRead_").append(typeName);
		writer.append("(nlohmann::json").reference().append("jsonIn, ");
		writer.append(typeName).reference().append("structOut)");
		writer.openBracket().endLine();
		writer.indent();
		writeReaderCode(writer, "jsonIn", "structOut");
		writer.unindent();
		writer.startLine().append('}');
	}
	
	public void writeReaderCode(CppWriter writer, String jsonVarName, String structName) {
		for(Group parent : parentTypes) {
			parent.writerReaderCall(writer, jsonVarName, structName);
		}

		if(!items.isEmpty()) {
			writer.startLine().append("for(nlohmann::json::iterator it = ");
			writer.append(jsonVarName).append(".begin(); it != ");
			writer.append(jsonVarName).append(".end(); ++it)");
			writer.openBracket().endLine();
			writer.indent();
			writer.startLine().append("std::string key = it.key();").endLine();
			writer.startLine().append("nlohmann::json val = it.value();").endLine();
			
			boolean checkedForASetting = false;
			for(SettingOrGroup item : items) {
				if(item.exclude) continue;
				
				String memberName = structName + '.' + item.codeName;
				
				if(checkedForASetting) {
					if(writer.newlineBrackets) {
						writer.endLine().startLine();
					} else {
						writer.append(' ');
					}
					writer.append("else ");
				} else {
					writer.startLine();
				}
				writer.append("if(key == \"").append(item.jsonName);
				writer.append("\")").openBracket().endLine();
				writer.indent();
				if(item.getClass() == Group.class) {
					Group group = (Group) item;
					// gonna rely on name shadowing
					group.writerReaderCall(writer, "val", memberName);
				} else {
					Setting setting = (Setting) item;
					setting.writerReaderCode(writer, "val", memberName);
				}
				writer.unindent();
				writer.startLine().append('}');
				
				checkedForASetting = true;
			}
			writer.endLine();
			
			// end of foreach
			writer.unindent();
			writer.startLine().append('}').endLine(); 
		}
	}
	
	public void writeWriterCode(CppWriter writer, String memberPrefix, boolean contentBefore) {
		if(!parentTypes.isEmpty()) {
			for(Group parent : parentTypes) {
				if(parent.isEmpty()) continue;
				
				writeWriterCode(writer, memberPrefix, contentBefore);
				contentBefore = true;
			}
		}
		
		if(!items.isEmpty()) {
			for(SettingOrGroup item : items) {
				if(item.exclude) continue;
				
				if(contentBefore) writer.append(',').endLine();
				
				writer.startLine().append('{');
				writer.append('"').append(item.jsonName).append("\", ");
				String cppName = memberPrefix + item.codeName;
				if(item.getClass() == Group.class) {
					writer.append('{');
					Group group = (Group) item;
					if(!group.isEmpty()) {
						writer.endLine();
						writer.indent();
						group.writeWriterCode(writer, cppName + '.', false);
						writer.endLine();
						writer.unindent();
						writer.startLine();
					}
					writer.append('}');
				} else {
					Setting setting = (Setting) item;
					if(setting.javaType == Enum.class) {
						writer.append("escgEnumV2S_").append(setting.cppType);
						writer.append('[').append(cppName).append(']');
					} else {
						writer.append(cppName);
					}
				}
				writer.append('}');
				contentBefore = true;
			}
		}
		if(contentBefore) writer.endLine();
	}
	
	public boolean isEmpty() {
		for(Group parent : parentTypes) {
			if(!parent.isEmpty()) return false;
		}
		return items.isEmpty();
	}
	
	private void addDependency(String type, List<String> out) {
		if(type != null && out != null && !out.contains(type)) {
			out.add(type);
		}
	}
	
	/**
	 * Get a list of structs/classes that need to be declared before this one
	 * So that it can properly compile.
	 * @param typeDependencies A list to store the dependencies in
	 */
	public void getDependencies(List<String> typeDependencies, List<String> enumDependencies) {
		// Add superclass dependencies
		for(Group group : parentTypes) {
			group.getDependencies(typeDependencies, enumDependencies);
			addDependency(group.typeName, typeDependencies);
		}
		
		for(SettingOrGroup item : items) {
			if(item.getClass() == Group.class) {
				Group group = (Group) item;
				addDependency(group.typeName, typeDependencies);
			} else {
				Setting setting = (Setting) item;
				if(setting.javaType == Enum.class) {
					addDependency(setting.cppType, enumDependencies);
				}
			}
		}
	}
	
	public List<SettingOrGroup> visibleItems() {
		// this is the easiest way
		// without turning everything into a pile of lambdas
		// or forcing the Java version clear up to 16
		List<SettingOrGroup> filtered = new ArrayList<>(items.size());
		for(SettingOrGroup item : items) {
			if(item.uiType != UIType.NONE) filtered.add(item);
		}
		return filtered;
	}
}