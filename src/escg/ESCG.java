package escg;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Stack;

public class ESCG {

	static boolean				supportImgui	= false;
	static boolean				supportImguiStd	= false;
	private static boolean		manualImgui		= false;
	private static boolean		includeImport	= true;
	private static List<String>	includes		= new ArrayList<>();

	private static String		mainType;
	private static String		outputName		= "escg";
	private static File			importFile;
	private static File			outputFile;
	
	/**
	 * flag to indicate that all parsing of the input file should be temporarily halted.<br>
	 * Exists so that the parser can be turned off for things that might break it
	 */
	private static boolean		noParse			= false;

	private static boolean isValidHeaderDeclaration(String include) {
		char s = include.charAt(0);
		char e = include.charAt(include.length() - 1);
		return (s == '"' && e == '"') || (s == '<' && e == '>');
	}
	
	private static void handleProperty(String prop, String value, Setting setting) {
		switch(prop) {
			// combined ui and exclude
			case "visibility":
				value = value.toLowerCase();
				switch(value) {
					case "json":
						setting.exclude = false;
						setting.uiType = UIType.NONE;
						break;
					case "ui":
						setting.exclude = true;
						if(setting.uiType == UIType.NONE) {
							setting.uiType = UIType.DEFAULT;
						}
						break;
					case "none":
						setting.exclude = true;
						setting.uiType = UIType.NONE;
						break;
					case "all":
						setting.exclude = false;
						if(setting.uiType == UIType.NONE) {
							setting.uiType = UIType.DEFAULT;
						}
						break;
				}
				break;
			case "min":
				setting.minValue = value;
				break;
			case "max":
				setting.maxValue = value;
				break;
			case "name":
				setting.friendlyName = value;
				break;
			case "desc":
			case "description":
				setting.description = value;
				break;
			case "validator":
				setting.validatorFunc = value;
				break;
			case "ui":
			case "uitype":
			case "ui_type":
			case "ui-type":
				UIType uiType = UIType.valueOf(value.toUpperCase());
				if(uiType != null) setting.uiType = uiType;
				break;
			case "exclude":
				setting.exclude = Utils.parseBoolean(value, setting.exclude);
				break;
			case "include":
				if(isValidHeaderDeclaration(value)) {
					includes.add(value);
					String lower = value.toLowerCase();
					if(lower.contains("imgui")) {
						if(lower.contains("stdlib")) {
							supportImguiStd = true;
						} else {							
							supportImgui = true;
						}
					}
					// don't include the scanned file twice
					if(lower.contains(importFile.getName().toLowerCase())) includeImport = false;
				} else {
					System.err.println("Invalid include declaration: " + value + ". Must be enclosed in brackets or quotes");
				}
				break;
			case "parse":
				noParse = !Utils.parseBoolean(value, !noParse);
		}
	}
	
	private static void parsePotentialProperty(Setting setting, String comment) {
		String meta = comment.substring(2);
		int propEnd = meta.indexOf(':');
		if(propEnd != -1) {
			String prop = meta.substring(0, propEnd).trim().toLowerCase();
			String value = meta.substring(propEnd + 1).trim();
			handleProperty(prop, value, setting);
		}
	}
	
	private static void writeIncludesAndDefines(CppWriter writer) {
		boolean wroteAnInclude = false;
		if(includeImport) { 
			writer.append("#include \"").append(importFile.getName());
			writer.append('"').endLine();
			wroteAnInclude = true;
		}
		for(String include : includes) {
			writer.append("#include ").append(include).endLine();
			wroteAnInclude = true;
		}
		if(wroteAnInclude) writer.endLine();
		if(supportImgui && !manualImgui) {
			if(!manualImgui) {
				writer.append("#define ESCG_IMGUI").endLine();
			}
			if(supportImguiStd) {
				writer.append("#define ESCG_IMGUI_STD").endLine();
			}
		}
		writer.append("#define ESCG_MAIN_TYPE ").append(mainType);
	}
	
	static void importStructs() {
		try(Scanner scanner = new Scanner(importFile)) {
			Setting itemBeingRead = new Setting();
			CppEnum enm = null;
			Stack<Group> hierarchy = new Stack<>();
			SimpleTokenizer line = new SimpleTokenizer();
			
			boolean clearSettings = false;
			while(scanner.hasNextLine()) {
				line.setLine(scanner.nextLine());
				// empty lines are useless
				if(line.isEmpty()) {
					clearSettings = true;
					continue;
				}
				
				// anything other than another comment-only line wipes state
				// state will persist for exactly 1 line past that
				// so it can be applied to a member before being cleared
				if(clearSettings) itemBeingRead.clearInfo();
				
				boolean fullComment = line.startsWithComment();
				String token = line.getRemaining();
				if(fullComment) {
					parsePotentialProperty(itemBeingRead, token);
					clearSettings = false;
					continue;
				}
				clearSettings = true;
				
				if(noParse) continue;
				
				// this comes after the comment check
				// so we can have these characters in the metadata
				if(line.hasBadCharacters()) continue;
				
				if(!hierarchy.isEmpty()) {
					switch(token) {
						case "public:":
							hierarchy.peek().currentlyPublic = true;
							continue;
						case "private:":
						case "protected:":
							hierarchy.peek().currentlyPublic = false;
							continue;
						default:
							if(token.startsWith("friend")) continue;
							if(token.startsWith("}")) {
								Group finishedItem = hierarchy.pop();
								int semicolon = token.indexOf(';');
								token = token.substring(1, semicolon).trim();
								if(!token.isEmpty()) {
									// if there was something in the middle,
									// then it must be the name of
									// an anonymous class/struct member
									finishedItem.setName(token);
									Group parent = hierarchy.peek();
									// null check because we can do that syntax even at root level
									if(parent != null && parent.currentlyPublic) {
										parent.items.add(finishedItem);
									}
								} else {
									// exit if we've read the main type
									// we don't support pointers
									// so everything needed to be read should have been read
									if(mainType.equals(finishedItem.typeName)) return;
								}
								continue;
							}
							token = line.nextSimpleToken();
							if(token.equals("struct")) {
								// anonymous structs might be more complex than this,
								// but if so, I'm not supporting that stuff
								Group group = new Group();
								group.copyInfo(itemBeingRead);
								hierarchy.push(group);
								continue;
							} else {
								Setting setting = new Setting();
								setting.copyInfo(itemBeingRead);
								setting.setType(token);
								line.finishParsingMember(setting);
								Group parent = hierarchy.peek();
								if(parent.currentlyPublic) parent.items.add(setting);
								continue;
							}
					}
				} else if(enm != null) {
					if(token.equals("}")) {
						enm = null;
						continue;
					} else {
						while(true) {
							String member = line.readEnumMember();
							if(member == null) break;
							enm.values.add(member);
							// I'm not sure, but this should actually work
							// without any additional effort
							String friendlyName = itemBeingRead.friendlyName;
							if(friendlyName == null) {
								friendlyName = Utils.snakeToFriendly(Utils.toSnakeCase(member));
							}
							enm.friendlyNames.put(member, friendlyName);
						}
					}
				} else {
					enm = line.getEnum();
					if(enm != null) continue;
				}
				
				boolean isStruct = false;
				token = line.nextSimpleToken();
				switch(token) {
					case "namespace":
						System.err.println("Namespaces not currently supported");
						break;
					case "struct":
						isStruct = true;
						// intentional fallthrough
					case "class":
						Group group = new Group();
						group.copyInfo(itemBeingRead);
						group.currentlyPublic = isStruct;
						line.finishParsingNamedClass(group);
						hierarchy.push(group);
						break;
				}
			}
			
		} catch(IOException ioe) {
			ioe.printStackTrace();
		}
	}
	
	// I've kinda given up on this implementation for the time being
	// While parsing individual C++ tokens is more appropriate,
	// it's WAY more effor than just reading line by line
	// and scanning for specific useful tokens -Liz (8/12/23)
	/*
	public void importStructs() {
		try(CppTokenReader reader = new CppTokenReader(new BufferedReader(new FileReader(importFile)))) {
			Setting itemBeingRead = new Setting();
			Group group = null;
			CppEnum enm = null;
			String namespace = null;
			LinkedList<String> tokens = new LinkedList<>();
			
			// contains the currently being read struct and parent structs
			List<Group> structNesting = new ArrayList<>();
			// brackets to expect that will end the current item
			// structs/classes are their own character that end when the item after them is closed
			// that way blocks not tied to structures can be parsed properly 
			List<Character> brackets = new ArrayList<>();
			while(true) {
				String token = reader.nextToken();
				if(token == null) break;
				
				char firstChar = token.charAt(0);

				if(firstChar == '/') {
					// skip block comments
					if(token.startsWith("/*")) continue;
					
					if(token.startsWith("//")) {
						parsePotentialProperty(itemBeingRead, token);
					}
				}
				
				// preprocessor definitions
				// for the love of god, please do not have conditional structs or stringify macros
				// hell, leave out defines too. they might work as parameters. they might not
				if(firstChar == '#') {
					reader.skipLine();
					continue;
				}
				
				if(enm != null) {
					enm.values.add(token);
					tokens.clear();
					reader.readToNextDelimiter(tokens);
					String delim = tokens.removeLast();
					if("}".equals(delim)) {
						enm = null;
					}
				} else if(group != null) {
					
				} else {
					switch(token) {
						case "static":
							break;
						case "enum":
							enm = new CppEnum();
							enm.namespace = namespace;
							enm.name = reader.nextToken();
							if(enm.name.equals("class")) {
								enm.isClass = true;
								enm.name = reader.nextToken();
							}
							tokens.clear();
							// will parse opening bracket and potential parent
							// parser won't stop at the : because its not
							// preceeded by a switch label or protection level
							reader.readToNextDelimiter(tokens);
							
							CppEnum.BY_NAME.put(enm.name, enm);
							break;
						case "struct":
						case "class":
							tokens.clear();
							reader.readToNextDelimiter(tokens);
							{
								String end = tokens.removeLast();
								if("{".equals(end)) {
									// otherwise it would be ;
									// indicating a forward declaration
									itemBeingRead.codeName = tokens.removeFirst();
									itemBeingRead.jsonName = Utils.toSnakeCase(itemBeingRead.codeName);
									group = new Group();
									itemBeingRead.copyInfo(itemBeingRead);
								}
							}
							break;
						case "namespace":
							tokens.clear();
							reader.readToNextDelimiter(tokens);
							{
								String end = tokens.removeLast();
								if("{".equals(end)) {
									// otherwise it would be ;
									// indicating a namespace alias(please no)
									
									String ns = tokens.removeFirst();
									namespace = namespace == null ? ns : namespace + "::" + ns;
									// TODO add namespace identifier to stack along with bracket
								}
							}
							break;
						default:
							break;
					}
				}
			}
		} catch(IOException e) {
			e.printStackTrace();
		}
	}
	*/
	
	private static void writeStructReaders(CppWriter writer) {
		List<String> allTypes = new ArrayList<>();
		
		allTypes.add(mainType);
		// Not using a foreach so that we can potentially do this
		// with with a single loop over a continually growing list.
		for(int i = 0; i < allTypes.size(); i++) {
			String type = allTypes.get(i);
			
			Group group = Group.BY_NAME.get(type);
			if(group == null) {
				System.err.println("Missing C++ type " + type);
				System.err.println("All types must exist directly within the input file");
				return;
			}
			
			for(SettingOrGroup item : group.items) {
				if(item.getClass() == Group.class) {
					Group subgroup = (Group) item;
					String name = subgroup.typeName;
					if(name != null && !allTypes.contains(name)) {
						allTypes.add(name);
					}
				}
			}
		}
		
		boolean wroteAStruct = false;
		List<String> dependencies = new ArrayList<>();
		List<String> writtenTypes = new ArrayList<>();
		while(!allTypes.isEmpty()) {
			Iterator<String> iter = allTypes.iterator();
			while(iter.hasNext()) {
				String type = iter.next();
				dependencies.clear();
				Group group = Group.BY_NAME.get(type);
				group.getDependencies(dependencies);
				if(writtenTypes.containsAll(dependencies)) {
					// put an empty line between each struct
					if(wroteAStruct) writer.endLine().endLine();
					
					group.writeReaderMethod(writer);
					
					wroteAStruct = true;
					iter.remove();
					writtenTypes.add(type);
				}
			}
		}
		
		/* Handling this using a chain of macros in the template instead
		writer.endLine();
		writer.append("#define ESCG_READ_MAIN_TYPE(data, obj) escgRead_");
		writer.append(mainType).append("(data, obj)").endLine();
		writer.endLine();
		//*/
	}
	
	private static void writeImguiTab(String tabName,
			List<SettingOrGroup> items, String containerPath, CppWriter writer)
	{
		writer.startLine().append("if(ImGui::BeginTabItem(\"").append(tabName);
		writer.append("\"))").openBracket().endLine();

		writer.indent();
		writeImguiGroupContent(items, containerPath, writer);
		writer.startLine().append("ImGui::EndTabItem();").endLine();
		writer.unindent();
		
		writer.startLine().append('}').endLine();
	}
	
	private static void writeImguiSetting(Setting setting,
			String containerPath, CppWriter writer)
	{
		// FIXME add description support
		String path = containerPath + setting.codeName;
		UIType uiType = UIType.getType(setting);
		String flags = "0";
		switch(uiType) {
			case CHECKBOX:
				writer.startLine().append("ImGui::Checkbox(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(");").endLine();
				break;
			case TEXT:
			case FILE:
				writer.startLine().append("ImGui::InputText(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(");").endLine();
				break;
			case NUMBER:
				writer.startLine().append("escgInputNumber(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(");").endLine();
				break;
			case LOGSLIDER:
				flags = "ImGuiSliderFlags_Logarithmic";
				// intentional fallthrough
			case SLIDER:
				writer.startLine().append("escgInputSlider(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(", ").append(setting.minValue);
				writer.append(", ").append(setting.maxValue).append(", ");
				writer.append(flags).append(");").endLine();
				break;
			case COMBOBOX:
				writeEnumComboBox(setting, path, writer);
				break;
				
		}
	}
	
	private static void writeEnumComboBox(Setting setting,
			String path, CppWriter writer)
	{
		CppEnum enm = CppEnum.BY_NAME.get(setting.cppType);
		String mapName = "escgEnumV2F_" + enm.name;
		
		// combo if
		writer.startLine().append("if(ImGui::BeginCombo(\"");
		writer.append(setting.friendlyName).append("\", ");
		writer.append(mapName).append('[').append(path).append("])");
		writer.openBracket().endLine();
		writer.indent();
		
		// foreach
		writer.startLine().append("for(const auto& pair : ").append(mapName);
		writer.append(')').openBracket().endLine();
		writer.indent();
		
		// begin foreach contents
		writer.startLine().append("const bool selected = ");
		writer.append(path).append(" == pair.first;").endLine();
		writer.append("if(ImGui::Selectable(pair.second.c_str(), selected)");
		writer.openBracket().endLine();
		writer.indent();
		writer.startLine().append(path).append(" = pair.first;").endLine();
		writer.unindent();
		writer.startLine().append('}').endLine();
		
		writer.startLine().append("if(selected)").openBracket().endLine();
		writer.indent();
		writer.startLine().append("ImGui::SetItemDefaultFocus();").endLine();
		writer.unindent();
		writer.startLine().append('}').endLine();
		// end foreach contents
		
		// end foreach
		writer.unindent();
		writer.startLine().append('}').endLine();
		
		// end combo if
		writer.unindent();
		writer.startLine().append('}').endLine();
	}
	
	private static void writeImguiGroupContent(List<SettingOrGroup> items,
			String containerPath, CppWriter writer)
	{
		for(SettingOrGroup item : items) {
			if(item.uiType == UIType.NONE) continue;
			
			if(item.getClass() == Group.class) {
				Group group = (Group) item;
				
				writer.startLine().append("if(ImGui::");
				// despite all the group options listed and documented in UiType,
				// only GROUP and TREE are currently supported
				writer.append(group.uiType == UIType.GROUP ?
						"CollapsingHeader" : "TreeNode");
				writer.append("(\"").append(group.friendlyName).append("\"))");
				writer.openBracket().endLine();
				
				String path = containerPath + group.codeName + '.';
				writer.indent();
				writeImguiGroupContent(group.items, path, writer);
				writer.unindent();
				writer.startLine().append('}').endLine();
			} else {
				writeImguiSetting((Setting) item, containerPath, writer);
			}
		}
	}
	
	private static void writeImguiCode(Group main, CppWriter writer) {
		writer.indent();
		// technically should be List<Setting>,
		// but it doesn't provide any benefit
		// and just complicates the generic parameters
		List<SettingOrGroup> filtered = main.visibleItems();
		List<SettingOrGroup> general = new ArrayList<>(main.items.size());
		// I'll keep the lambda for now in case I decide to go back to Streams
		int mainCount = filtered.size();
		filtered.forEach(item -> {
			if(item.getClass() == Setting.class) {
				general.add(item);
			}
		});
		// don't create a general tab if there's no settings in the root object
		boolean noGeneral = general.isEmpty();
		// don't create tabs if there are no substructs,
		// or if the root only consists of a single substruct
		boolean useTabs = noGeneral ? mainCount > 1 : general.size() < mainCount;
		if(useTabs) {
			writer.startLine().append("if(ImGui::BeginTabBar(\"Tabs\"))");
			writer.openBracket().endLine();
			writer.indent();
			if(!noGeneral) {
				writeImguiTab("General", general, "obj.", writer);
			}
			for(SettingOrGroup item : filtered) {
				if(item.getClass() == Group.class) {
					Group group = (Group) item;
					String path = "obj." + group.codeName + '.';
					writeImguiTab(group.friendlyName, group.visibleItems(), path, writer);
				}
			}
			writer.startLine().append("ImGui::EndTabBar();").endLine();
			writer.unindent();
			writer.startLine().append('}').endLine();
		} else {
			if(noGeneral) {
				// !useTabs && noGeneral dictates main.items consisnts of
				// a single Group object and nothing else
				Group group = (Group) main.items.get(0);
				String path = "obj." + group.codeName + '.';
				writeImguiGroupContent(group.items, path, writer);
			} else {
				// !useTabs && !noGeneral dictates main.items only consists settings
				writeImguiGroupContent(general, "obj.", writer);
			}
		}
		writer.unindent();
	}
	
	private static final String PRAGMA_ESCG = "#pragma escg(";
	
	private static void createCppFile(Group main) throws IOException {
		
		String template = Files.readString(new File("code_template.cpp").toPath());
		CppWriter writer = new CppWriter();
		writer.determineLineSeparatorFromReference(template);
		String[] lines = template.split(writer.lineSeparator);
		writer.determineIndentFromReference(lines);
		boolean hadPrevLine = false;
		for(String line : lines) {
			if(hadPrevLine) writer.endLine();
			
			// has to be non-null so the switch statement works
			String replacement = "DUMMY_TEXT";
			if(!line.isEmpty() && line.charAt(0) == '#') {
				if(line.startsWith(PRAGMA_ESCG)) {
					int idx = line.indexOf(')');
					if(idx != -1) {
						replacement = line.substring(PRAGMA_ESCG.length(), idx).toLowerCase();
					}
				}
				
				// allow imgui to be supported 
				if(line.startsWith("#include ")) {
					String lower = line.toLowerCase();
					if(lower.contains("imgui")) {
						if(lower.contains("stdlib")) {
							supportImguiStd = true;
						} else {							
							supportImgui = true;
						}
					}
					if(lower.contains(importFile.getName().toLowerCase())) includeImport = false;
				}
				
				if(line.startsWith("#define ESCG_IMGUI")) {
					if(supportImgui) {
						System.err.println("YOU WERE SPECIFICALLY TOLD IN THE TEMPLATE " +
								"NOT TO MANUALLY PUT IN THE IMGUI DEFINE");
						line = "";
					} else {
						manualImgui = true;
					}
				}
			}
			
			switch(replacement) {
				case "includes":
					writeIncludesAndDefines(writer);
					break;
				case "serialize":
				case "write":
					writer.indent().indent();
					main.writeWriterCode(writer, "obj.", false);
					writer.unindent().unindent();
					break;
				case "deserialize":
				case "read":
					writeStructReaders(writer);
					break;
				case "enum":
				case "enums":
					CppEnum.writeAll(writer);
					break;
				case "imgui":
					if(supportImgui) {
						writeImguiCode(main, writer);
					} else {
						writer.startLine().append("// imgui code excluded. add an imgui import to include it");
					}
					break;
				default:
					writer.append(line);
					break;
			}
			
			hadPrevLine = true;
		}
		
		Files.writeString(outputFile.toPath(), writer.toString());
	}

	public static void main(String[] args) {
		if(args == null || args.length != 2) {
			System.err.println("Need 2 arguments: input_file and main_type");
			return;
		}
		
		importFile = new File(args[0]);
		if(!importFile.exists() || !importFile.canRead()) {
			System.err.println("File specified is not an existing readable file");
			return;
		}
		
		mainType = args[1];
		importStructs();
		Group main = Group.BY_NAME.get(mainType);
		if(main == null) {
			System.err.println("C++ file parsed did not contain the type indicated");
			return;
		}
		
		File dir = importFile.getParentFile();
		outputFile = new File(dir, outputName + ".cpp");
		try {
			createCppFile(main);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}
		
		// TODO potentially write a header as well
	}
}