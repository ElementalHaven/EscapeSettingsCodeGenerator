package escg;

import java.io.File;
import java.io.IOException;

import java.nio.file.Files;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;
import java.util.Stack;

public class ESCG {

	static boolean				supportImgui		= false;
	static boolean				supportImguiStd		= false;
	private static boolean		manualImgui			= false;
	private static List<String>	includes			= new ArrayList<>();

	private static String		nlohmannHeader;
	private static Boolean		newlineBrackets		= null;
	private static Boolean		referenceBefore		= null;
	private static boolean		supportFilesystem	= false;

	/**
	 * flag to indicate that all parsing of the input file should be temporarily halted.<br>
	 * Exists so that the parser can be turned off for things that might break it
	 */
	private static boolean		noParse				= false;
	
	private static String getPathType() {
		return supportFilesystem ? "std::filesystem::path" : "std::string";
	}

	private static boolean isValidIncludeDeclaration(String include) {
		char s = include.charAt(0);
		char e = include.charAt(include.length() - 1);
		return (s == '"' && e == '"') || (s == '<' && e == '>');
	}
	
	private static void handleSpecialIncludes(String include, IOGrouping io) {
		String lower = include.toLowerCase().replace('\\', '/');
		if(lower.contains("imgui")) {
			if(lower.contains("stdlib")) {
				supportImguiStd = true;
			}
			// imgui_stdlib includes imgui, so both are valid
			supportImgui = true;
		}
		if(lower.contains("nlohmann") || lower.contains("json")) {
			nlohmannHeader = include;
		}
		if("<filesystem>".equals(lower)) supportFilesystem = true;
		
		// don't include the scanned file twice
		Iterator<String> iter = io.unimportedFiles.iterator();
		while(iter.hasNext()) {
			String str = iter.next();
			if(str.equalsIgnoreCase(lower)) {
				iter.remove();
				break;
			}
		}
	}
	
	private static void handleProperty(String prop, String value,
			Setting setting, IOGrouping io)
	{
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
				if(setting.description == null) {
					setting.description = value;
				} else {
					// support descriptions spanning multiple lines
					// (any way you want to interpret that)
					if(!setting.description.endsWith("\\n")) {
						setting.description += ' ';
					}
					setting.description += value;
				}
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
				if(isValidIncludeDeclaration(value)) {
					includes.add(value);
					handleSpecialIncludes(value, io);
				} else {
					System.err.println("Invalid include declaration: " + value +
							". Must be enclosed in brackets or quotes");
				}
				break;
			case "parse":
				noParse = !Utils.parseBoolean(value, !noParse);
				break;
			case "newline-brackets":
			case "newline_brackets":
			case "newlinebrackets":
				newlineBrackets = Utils.parseBoolean(value, newlineBrackets);
				break;
			case "reference_before":
			case "reference-before":
			case "referencebefore":
			case "ref-before":
			case "ref_before":
			case "refbefore":
				referenceBefore = Utils.parseBoolean(value, referenceBefore);
				break;
			case "ref":
			case "refs":
			case "reference":
			case "references":
				value = value.toLowerCase();
				switch(value) {
					case "before":
					case "type":
					case "left":
						referenceBefore = true;
						break;
					case "after":
					case "name":
					case "right":
						referenceBefore = false;
						break;
				}
				break;
			case "bracket":
			case "brackets":
			case "openingbracket":
			case "opening-bracket":
			case "opening_bracket":
			case "openingbrackets":
			case "opening-brackets":
			case "opening_brackets":
				value = value.toLowerCase();
				switch(value) {
					case "newline":
					case "new-line":
					case "new line":
					case "new_line":
						newlineBrackets = true;
						break;
					case "sameline":
					case "same-line":
					case "same line":
					case "same_line":
						newlineBrackets = false;
						break;
				}
				break;
			case "filesystem":
			case "std-filesystem":
				supportFilesystem = Utils.parseBoolean(value, supportFilesystem);
				break;
			case "output":
			case "outname":
				io.outputName = value;
				break;
		}
	}
	
	private static void parsePotentialProperty(Setting setting, String comment,
			IOGrouping io)
	{
		String meta = comment.substring(2);
		int propEnd = meta.indexOf(':');
		if(propEnd != -1) {
			String prop = meta.substring(0, propEnd).trim().toLowerCase();
			String value = meta.substring(propEnd + 1).trim();
			handleProperty(prop, value, setting, io);
		}
	}
	
	private static void writeHeaderIncludes(IOGrouping io) {
		CppWriter writer = io.writer;
		for(String include : includes) {
			writer.append("#include ").append(include).endLine();
		}
		for(String path : io.unimportedFiles) {
			writer.append("#include ").append(path).endLine();
		}
	}
	
	private static void writeIncludesAndDefines(IOGrouping io) {
		CppWriter writer = io.writer;
		writer.append("#include \"").append(io.outputName).append(".h\"");
		writer.endLine();
		if(supportImgui && !manualImgui) {
			writer.endLine();
			if(!manualImgui) {
				writer.append("#define ESCG_IMGUI").endLine();
			}
			if(supportImguiStd) {
				writer.append("#define ESCG_IMGUI_STD").endLine();
			}
		}
	}
	
	static void importStructs(IOGrouping io) {
		for(File f : io.inputFiles) {
			importStructs(f, io);
		}
	}
	
	static void importStructs(File importFile, IOGrouping io) {
		try(Scanner scanner = new Scanner(importFile)) {
			Setting itemBeingRead = new Setting();
			CppEnum enm = null;
			Stack<Group> hierarchy = new Stack<>();
			SimpleTokenizer line = new SimpleTokenizer();
			
			boolean clearSettings = false;
			//int lineNo = 0;
			while(scanner.hasNextLine()) {
				// exists for debugging purposes
				String initialLine = scanner.nextLine();
				//lineNo++;
				
				line.setLine(initialLine);
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
					parsePotentialProperty(itemBeingRead, token, io);
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
								int semicolon = token.indexOf(';');
								// we read an inline/template method end
								// for the love of god,
								// don't stick useless semicolons after them
								if(semicolon == -1) continue;
								
								Group finishedItem = hierarchy.pop();
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
	
	private static void writeStructReaders(IOGrouping io) {
		boolean wroteAStruct = false;
		for(String type : io.typesToExport) {
			// put an empty line between each struct
			if(wroteAStruct) io.writer.endLine().endLine();
			
			Group group = Group.BY_NAME.get(type);
			group.writeReaderMethod(io.writer);
			
			wroteAStruct = true;
		}
	}
	
	private static void writeConvenienceReaders(IOGrouping io) {
		CppWriter writer = io.writer;
		String pathType = getPathType();
		for(String type : io.mainTypes) {
			// put an empty line between each method
			io.writer.endLine().endLine();

			writer.startLine().append("bool escgReadFromFile(const ");
			writer.append(pathType).reference().append("filename, ").append(type);
			writer.reference().append("obj)").openBracket().endLine();
			writer.indent();
			if(supportFilesystem) {
				writer.startLine();
				writer.append("if(!std::filesystem::is_regular_file(filename)) ");
				writer.append("return false;").endLine();
			} else {
				writer.startLine().append("// unable to check if file exists ");
				writer.append("in platform agnostic way as ");
				writer.append("std filesystem was not included").endLine();
			}
	
			writer.endLine();
			
			writer.startLine().append("std::ifstream stream(filename);").endLine();
			writer.startLine().append("nlohmann::json data = ");
			writer.append("nlohmann::json::parse(stream, nullptr, true, true);");
			writer.endLine();
			writer.startLine().append("escgRead_").append(type);
			writer.append("(data, obj);").endLine().endLine();
			writer.startLine().append("return true;").endLine();
			
			writer.closeBracketLine();
		}
	}
	
	private static void writeImguiTab(String tabName,
			List<SettingOrGroup> items, String containerPath, CppWriter writer)
	{
		writer.startLine().append("if(ImGui::BeginTabItem(\"").append(tabName);
		writer.append("\"))").openBracket().endLine();

		writer.indent();
		writeImguiGroupContent(items, containerPath, writer);
		writer.startLine().append("ImGui::EndTabItem();").endLine();

		writer.closeBracketLine();
	}
	
	private static void writeImguiSetting(Setting setting,
			String containerPath, CppWriter writer)
	{
		String path = containerPath + setting.codeName;
		UIType uiType = UIType.getType(setting);
		String flags = "0";
		switch(uiType) {
			case CHECKBOX:
				writer.startLine().append("modified |= ImGui::Checkbox(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(");").endLine();
				break;
			case TEXT:
			case FILE:
				writer.startLine().append("modified |= ImGui::InputText(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(");").endLine();
				break;
			case NUMBER:
				writer.startLine().append("modified |= escgInputNumber(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(");").endLine();
				break;
			case COLOR:
				writer.startLine().append("modified |= escgInputColor(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(");").endLine();
				break;
			case LOGSLIDER:
				flags = "ImGuiSliderFlags_Logarithmic";
				// intentional fallthrough
			case SLIDER:
				writer.startLine().append("modified |= escgInputSlider(\"");
				writer.append(setting.friendlyName).append("\", &");
				writer.append(path).append(", ").append(setting.minValue);
				writer.append(", ").append(setting.maxValue).append(", ");
				writer.append(flags).append(");").endLine();
				break;
			case COMBOBOX:
				writeEnumComboBox(setting, path, writer);
				break;	
		}
		if(setting.description != null) {
			writer.startLine().append("if(ImGui::IsItemHovered())").openBracket();
			writer.endLine();
			writer.indent();
			writer.startLine().append("ImGui::BeginTooltip();").endLine();
			//writer.startLine().append("ImGui::PushTextWrapPos(");
			//writer.append("ImGui::GetFontSize() * 35.0f);").endLine();
			writer.startLine().append("ImGui::TextUnformatted(\"");
			writer.append(setting.description).append("\");").endLine();
			//writer.startLine().append("ImGui::PopTextWrapPos();").endLine();
			writer.startLine().append("ImGui::EndTooltip();").endLine();
			writer.closeBracketLine();
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
		writer.startLine().append("for(const auto").reference().append("pair : ");
		writer.append(mapName).append(')').openBracket().endLine();
		writer.indent();
		
		// begin foreach contents
		writer.startLine().append("const bool selected = ");
		writer.append(path).append(" == pair.first;").endLine();
		writer.append("if(ImGui::Selectable(pair.second.c_str(), selected)");
		writer.openBracket().endLine();
		writer.indent();
		writer.startLine().append(path).append(" = pair.first;").endLine();
		writer.startLine().append("modified |= !selected;").endLine();
		writer.closeBracketLine();
		
		writer.startLine().append("if(selected)").openBracket().endLine();
		writer.indent();
		writer.startLine().append("ImGui::SetItemDefaultFocus();").endLine();
		writer.closeBracketLine();
		// end foreach contents
		
		// end foreach
		writer.closeBracketLine();
		
		// end combo if
		writer.closeBracketLine();
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
				boolean isTree = group.uiType != UIType.GROUP;
				writer.append(isTree ? "TreeNode" : "CollapsingHeader");
				writer.append("(\"").append(group.friendlyName).append("\"))");
				writer.openBracket().endLine();
				
				String path = containerPath + group.codeName + '.';
				writer.indent();
				writeImguiGroupContent(group.items, path, writer);
				if(isTree) {
					writer.startLine().append("ImGui::TreePop();").endLine();
				}
				writer.closeBracketLine();
			} else {
				writeImguiSetting((Setting) item, containerPath, writer);
			}
		}
	}
	
	private static void writeImguiCode(IOGrouping io) {
		CppWriter writer = io.writer;
		boolean wroteAMethod = false;
		for(String type : io.mainTypes) {
			if(wroteAMethod) writer.endLine();
			
			Group group = Group.BY_NAME.get(type);

			writer.startLine().append("bool escgShowUI(").append(type);
			writer.reference().append("obj, bool* open)").openBracket().endLine();
			writer.indent();
			writer.startLine().append("if(!ImGui::Begin(\"Settings\", open))");
			writer.openBracket().endLine();
			writer.indent();
			writer.startLine().append("ImGui::End();").endLine();
			writer.startLine().append("return false;").endLine();
			writer.closeBracketLine().endLine();
			writer.startLine().append("bool modified = false;").endLine();
			
			writeImguiCode(group, writer);
			
			writer.startLine().append("ImGui::End();").endLine();
			writer.startLine().append("return modified;").endLine();
			writer.closeBracketLine();
		}
	}
	
	private static void writeImguiCode(Group main, CppWriter writer) {
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
			writer.closeBracketLine();
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
	}
	
	private static void writeWriterCode(IOGrouping io) {
		boolean wroteAMethod = false;
		CppWriter writer = io.writer;
		String pathType = getPathType();
		for(String type : io.mainTypes) {
			if(wroteAMethod) writer.endLine();
			
			// method to write to json
			writer.startLine().append("nlohmann::json escgToJson(const ");
			writer.append(type).reference().append("obj)").openBracket().endLine();
			
			writer.indent();
			writer.startLine().append("nlohmann::json ret").openBracket().endLine();
			writer.indent();
			Group group = Group.BY_NAME.get(type);
			group.writeWriterCode(writer, "obj.", wroteAMethod);
			writer.unindent();
			writer.startLine().append("};").endLine();
			writer.startLine().append("return ret;").endLine();
			writer.closeBracketLine();
			writer.endLine();
			
			// method to write to file
			writer.startLine().append("void escgWriteToFile(const ").append(pathType);
			writer.reference().append("path, const ").append(type).reference();
			writer.append("obj)").openBracket().endLine();
			writer.indent();
			writer.startLine().append("nlohmann::json data = escgToJson(obj);").endLine();
			writer.startLine().append("std::ofstream stream(path);").endLine();
			writer.startLine().append("stream << data;").endLine();
			writer.closeBracketLine();
		}
	}
	
	private static final String PRAGMA_ESCG = "#pragma escg(";
	
	private static void createCppFile(IOGrouping files) throws IOException {
		String template = Files.readString(new File("code_template.cpp").toPath());
		CppWriter writer = files.writer;
		writer.determineLineSeparatorFromReference(template);
		String[] lines = template.split(writer.lineSeparator);
		writer.determineIndentFromReference(lines);
		
		// allow the user to specify rather than being fully reliant on autodetection
		if(referenceBefore != null) writer.referenceBefore = referenceBefore;
		if(newlineBrackets != null) writer.newlineBrackets = newlineBrackets;
		
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
				
				// allow imgui to be supported, among other things
				if(line.startsWith("#include ")) {
					handleSpecialIncludes(line.substring(9), files);
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
			
			hadPrevLine = false;
			switch(replacement) {
				case "includes":
					writeIncludesAndDefines(files);
					break;
				case "serialize":
				case "write":
					writeWriterCode(files);
					break;
				case "deserialize":
				case "read":
					writeStructReaders(files);
					writeConvenienceReaders(files);
					break;
				case "enum":
				case "enums":
					CppEnum.writeAll(files);
					break;
				case "imgui":
					if(supportImgui) {
						writeImguiCode(files);
					} else {
						writer.startLine().append("// imgui code excluded. add an imgui import to include it");
					}
					break;
				default:
					writer.append(line);
					hadPrevLine = true;
					break;
			}
		}
		
		Files.writeString(files.outputFile.toPath(), writer.toString());
	}
	
	private static void writeHeader(IOGrouping io) throws IOException {
		String pathType = getPathType();
		
		final CppWriter writer = io.writer;
		writer.clear();
		writer.append("#pragma once").endLine();
		writer.append("/************************************************").endLine();
		writer.append(" * THIS FILE IS MACHINE GENERATED. DO NOT EDIT. *").endLine();
		writer.append(" ************************************************/").endLine();
		writer.endLine();
		
		// included headers
		if(supportFilesystem) {
			writer.append("#include <filesystem>").endLine();
		} else {
			writer.append("#include <string>").endLine();
		}
		writer.append("#include <fstream>").endLine();
		writeHeaderIncludes(io);
		if(nlohmannHeader == null) {
			writer.append("// Nlohmann header was assumed. If this is wrong, Include it in ");
			writer.append("either an input property or stick it in the template").endLine();
			writer.append("#include <nlohmann/json.hpp>").endLine();
		} else {
			writer.append("#include ").append(nlohmannHeader).endLine();
		}
		
		for(String mainType : io.mainTypes) {
			writer.endLine();
			
			writer.append("// ").append(mainType).append(" -----------").endLine();
			
			writer.append("nlohmann::json escgToJson(const ").append(mainType);
			writer.reference().append("obj);").endLine();
			
			writer.append("void escgWriteToFile(const ").append(pathType).reference();
			writer.append("path, const ").append(mainType).reference().append("obj);").endLine();
			
			writer.append("bool escgReadFromFile(const ").append(pathType).reference();
			writer.append("filename, ").append(mainType).reference().append("obj);").endLine();
			
			writer.append("void escgRead_").append(mainType).append("(nlohmann::json");
			writer.reference().append("jsonIn, ").append(mainType).reference();
			writer.append("structOut);").endLine();
			
			if(supportImgui) {
				writer.append("bool escgShowUI(").append(mainType).reference();
				writer.append("obj, bool* open = nullptr);").endLine();
			}
		}
		Files.writeString(io.outputHeader.toPath(), writer.toString());
	}

	public static void main(String[] args) {
		IOGrouping files = new IOGrouping();
		
		for(String arg : args) {
			boolean isFile = Utils.isCppFile(arg);
			if(isFile) files.inputFiles.add(new File(arg));
			else files.mainTypes.add(arg);
		}
		
		if(!files.preliminaryValidation()) return;

		importStructs(files);
		if(!files.lateValidation()) return;
		
		try {
			createCppFile(files);
			writeHeader(files);
		} catch(IOException e) {
			e.printStackTrace();
			return;
		}		
	}
}