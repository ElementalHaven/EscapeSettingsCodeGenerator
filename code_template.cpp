/**
 * This file is machine generated. Do not edit.
 */
#include <map>
#include <string>
#include <filesystem>
#include <fstream>

#pragma escg(includes)

#pragma escg(enums)

#pragma escg(deserialize)

// ESCG_IMGUI is automatically defined in the includes section
// if imgui is detected as being included
// do not define it manually
#ifdef ESCG_IMGUI

void escgShowUI(ESCG_MAIN_TYPE& obj) {
	if(!ImGui::Begin("Settings", &enabled)) {
		ImGui::End();
		return;
	}
#pragma escg(imgui)
	ImGui::End();
}

#endif /* ESCG_IMGUI */

#define ESCG_MAIN_TYPE Settings

nlohmann::json escgToJson(const ESCG_MAIN_TYPE& obj) {
	nlohmann::json ret {
#pragma escg(serialize)
	};
	return ret;
}

void escgWriteToFile(const std::filesystem::path& path, const ESCG_MAIN_TYPE& obj) {
	nlohman::json data = escgToJson(obj);
	std::ofstream stream(filename);
	stream << data;
}

// these exist so the main type's reader method can be called
// without another custom preprocessor directive
// and so the relevant define is present in the template and not just generated code
// that way people customizing this template can understand what's going on
// without having to dig through ESCG's code
// the inner method exists because of macro resolution order
// macro variables are resolved first
// UNLESS they're used with concatenation or stringify operators
#define ESCG_READ_INNER(typeName, data, obj) escgRead_ ## typeName(data, obj)
#define ESCG_READ(typeName, data, obj) ESCG_READ_INNER(typeName, data, obj)
#define ESCG_READ_MAIN_TYPE(data, obj) ESCG_READ(ESCG_MAIN_TYPE, data, obj)

bool escgReadFromFile(const std::filesystem::path& filename, ESCG_MAIN_TYPE& obj) {
	if(!std::filesystem::is_regular_file(filename)) return false;

	std::ifstream stream(filename);
	nlohmann::json data = nlohmann::json::parse(stream, nullptr, true, true);
	ESCG_READ_MAIN_TYPE(data, obj);

	return true;
}