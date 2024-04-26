# Escape Settings Code Generator

Utility for generating code to read and write structs to and from JSON, along with showing an [ImGui](https://github.com/ocornut/imgui) UI for editing the structs

## Requirements

Requires JDK 11 or newer to build / Java 11 or newer to run

## Program Syntax

`java -jar ESCG.jar myheader.h MyStructName`

Any parameter ending in .h or .hpp will be considered an input file and the program will scan it for any necessary types.
The full path to the input file should be provided so that the program can find and read it. Paths relative to the current working redirectory are also supported.
All other parameters will be considered a class or struct name that the user wishes to have methods generated for.
Any number of header files and struct names can be specified. While order does not currently matter, it is advised to stick to the convention of listing all input files first and all struct/class names. This program currently only exports a single file, however support for exporting multiple files has been considered using input files coming after struct/class names to denote the start of a new group to export to a different file.

## Annotations

To provide additional information about struct members, header files to include in the generated files, and other things, annotations can be used.  
Basic syntax:
`// name: value`

Leading and trailing spaces are discarded, both for annotation names and for values. Additionally, annotation names are case insensitive.
Quotes have no special meaning in either the names or values. Values with spaces in the middle are used in their entirety.
If you need to have a text string with leading or trailing spaces, you should define a macro containing the string, include it with an annotation, and specify the macro enclosed in quotes as the value, eg: `// desc: " MY_MACRO "`

Because values are generally pasted into the code verbatim, using macros and even functions generally works fine.

Annotations meant for member variables apply only to the next line that isn't an annotation/single line comment(`//`).

### Member Variable Annotations
- **name**  
	&emsp;The name of the struct or class member as it will appear in UIs or console/log messages.
	While optional, setting this is highly recommended. If not present, the variable's name converted to snake_case will be used.
- **desc**  
  **description**  
	&emsp;Provides additional information about the variable that might be shown to the user with a tooltip or other mechanism.  Specifying this multiple times for a single member will result in the text from all description annotations being concatenated together.
- **min**  
	&emsp;The minimum allowed value for a numeric type. The value specified is used directly as code so it can be an inlined value, constant, macro, or even a function call.
- **max**  
	&emsp;The maximum allowed value for a numeric type. The value specified is used directly as code so it can be an inlined value, constant, macro, or even a function call.
<!-- - **validator**  
	&emsp;The name of a function that is called on the value after bounds checking is done to ensure that the value meets additional requirements.
	It may also be used to calculate derived values or to send the values to some API to be applied.  
	function syntax is currently to be decided.  -->
- **ui**  
  **uitype**  
  **ui_type**  
  **ui-type**  
    &emsp;Specifies what mechanism should be used to give the user control over this type in an ImGui UI. If not specified, the UI control will be determined from type and whether minimum and maximum values have been specified. Empty groups will be excluded from UIs automatically but will still be present in the json. Only the last annotation affecting UI type will apply. This includes `visibility` annotations. Value parsing is case-insensitive.  
	Supported types:
	| Name | Usable By | Appearance | Default For |
	| ---- | --------- | ---------- | ----------- |
	| default | All | A number with increment and decrement buttons. | Not applicable. |
	| none | All | Item is not present in the UI. | None. |
	| text | `std::string` (`char *` is not currently supported) | A simple textbox. | `std::string` |
	| number | Numeric types | A textbox with increment and decrement buttons. | Numeric types without min and max annotations. |
	| slider | Numeric types with min and max annotiations. | A slider. | Numeric types with min and max annotiations. |
	| logslider | Numeric types with min and max annotiations. | A logarithmically-scaled slider. | None. |
	| checkbox | `bool` | A checkbox. | `bool` |
	| color | `int`, `int32_t`, `std::string`, `glm::vec3`, `glm::vec4` | A color picker without alpha. | `glm::vec3`, `glm::vec4` |
	| combobox | `enum` | A dropdown menu. | `enum` |
	| group | `class`, `struct` | A section with a large header. | None. |
	| tab | `class`, `struct` | A section confined to a tab. | `class`es and `struct`s contained directly by the root object |
	| tree | `class`, `struct` | A collapsible, headerless section. | `class`, `struct` |
	| custom | All | Meant to provide the user with a means of manually handling variables.<br/>Currently unimplemented and will function the same as `none` as a result. | None. |

	Numeric types include: `float`, `double`, and any type whose name contains "int".  
	Types with namespace prefixes are generally checked by simply seeing if the type name contains the unprefixed value. Other types containing said text will likely also be seen as said types.
- **visibility**  
	&emsp;Simplified annotation that's the equivalent of specifying both `ui` and `exclude`. If this annotation forces the item to be present in the UI, then the UI type will be set to `default` if and only if the existing UI type is `none`. Otherwise, the existing UI type is maintained. Values are case-insensitve.  
	Possible values:  
	| Value | Present in UI | Present in JSON |
	| - | - | - |
	| json | No | Yes |
	| ui | Yes | No |
	| none | No | No |
	| all | Yes | Yes |
- **exclude**  
	&emsp;A boolean value controlling whether or not this member is present in the JSON. All public member variables default to true.
- **validator**  
	&emsp;The name of a user-defined global function that will be used to validate the member.  
	> ⚠️ **Warning** - This feature is currently unimplemented and has no format or syntax defined.

### Formatting Annotations
- **ref**  
  **refs**  
  **reference**  
  **references**  
	&emsp;Controls whether the symbol for references(&amp;) is placed next to the type name or variable name. Values of `before`, `type`, and `left` will cause references to be placed by the type. Values of `after`, `name` and `right` will cause references to be placed by the variable name. Value parsing is case-insenitive.
- **reference_before**  
  **reference-before**  
  **referencebefore**  
  **ref-before**  
  **ref_before**  
  **refbefore**  
	&emsp;Boolean indicating whether references should come before or after the space separating type and variable names. As the annotation name implies, values equating to `true` will case the reference symbol to be placed by the type name and `false` will cause them to be placed by the variable name.
- **bracket**  
  **brackets**  
  **openingbracket**  
  **opening-bracket**  
  **opening_bracket**  
  **openingbrackets**  
  **opening-brackets**  
  **opening_brackets**  
	&emsp;Annotation indicating whether opening brackets for control flow statements should be placed on the same line as the statement or on a new line. A value of `newline` indicates they should be on a new line while a value of `sameline` indicates they should be on the same line. Value parsing is case-insensitive and a space, underscore, or dash is permitted before `line` in the value name.
- **newline-brackets**  
  **newline-brackets**  
  **newlinebrackets**  
	&emsp;Boolean indicating whether opening brackets for control flow statements should be placed on the same line as the statement or on a new line. As the annotation name imples, values equating to `true` will cause them to be placed on a new line and values equating to `false` will cause them to be placed on the same line.

### General Annotations
- **include**  
	&emsp;Specifies a file to be included in a `#include` declaration. Do not include the `#include` directive. The file must be enclosed in either quotes(`"`) or angled brackets(`<>`) as one would for an actual include statement.  
	&emsp;A number of include statements have special purpose:
	+ An include containing "nlohmann" or "json" in the name will be assumed to be the json library header. If not present in any specified file, a default header of `<nlohmann/json.hpp>` will be assumed and included.
	+ An include containing "imgui" will be assumed to be the ImGui header and will be used to enable the generation of ImGui code. ImGui code will not be generated if no ImGui header is specified. Additionally, if this include contains "stdlib", the generated code will assume the file contains `ImGui::InputText`. If such a file is not specified, the generated code will use its own implementation of this method for the purposes of supporing `std::string`.
	+ An include exactly matching `<filesystem>` will be assumed to be the `std::filesystem` header. Its presense will cause generated methods for reading and writing json files to use `std::filesystem::path` as a parameter type for the filename rather than `std::string`.  

	&emsp;It should be noted that all the above rules use case-insensitive checking.
- **filesystem**  
  **std-filesystem**  
	&emsp;Value will be interpreted as a boolean indicating whether or not to use `std::filesystem`.
	A value resolving to true will have the same result as `// include: <filesystem>`
- **output**  
  **outname**  
	&emsp;Specifies the name of the file containing the generated code, without path or extension. Both a `.h` and `.cpp` file will be placed in the same folder as the first input file specified. If not specified, the name of these files defaults to "escg".
- **parse**  
	&emsp;A boolean value enabling or disabling the parsing of member variables. After a `// parse: false` annotation, no new member variables will be added to types until a `// parse: true` annotation is encountered. This exists mainly to circumvent sections of code that the parser might not understand.

## Limitations
- As the methods created by ESCG are not class members, only public member variables can be used. Any protected or private members are treated as if they have a `// visibility: none` annotation.
- It is important to specify both the file containing the specified structs along with any other headers that are needed for types of member variables in the command line parameters. ESCG will not make any attempt to read headers included by the input files.
- The parser is fairly simple. As such, it can not understand whether or not any sections of code are gated off with processor statements such as `#ifdef`
- The JSON library used by ESCG is [nlohmann](https://github.com/nlohmann/json). It is not included with ESCG. No other JSON libraries are currently supported.
- Neither pointer or reference types are supported for struct or class members.
- Namespace blocks are not understood by the parser. All namespace support is generally hardcoded cases for specific classes.
- Arrays are not supported.
- `char`, `short`, `long`, and `long long` types will likely not work. It is advised to use stdint or __intX types instead.
