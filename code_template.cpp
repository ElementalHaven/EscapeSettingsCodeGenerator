/************************************************
 * THIS FILE IS MACHINE GENERATED. DO NOT EDIT. *
 ************************************************/
#include <map>
#include <string>
#include <inttypes.h>
#include <filesystem>
#include <fstream>

#pragma escg(includes)

#pragma escg(enums)

#pragma escg(deserialize)

// ESCG_IMGUI is automatically defined in the includes section
// if imgui is detected as being included
// do not define it manually
#ifdef ESCG_IMGUI

// These exist to provide an overloaded method for numeric inputs.
// rather than having the generator track the exact numeric type,
// it becomes the  C++ compiler's job to do that instead
// currently there's no step support but maybe I'll put that in one day

inline bool escgInputNumber(const char* label, int8_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_S8, data);
}
inline bool escgInputNumber(const char* label, uint8_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_U8, data);
}
inline bool escgInputNumber(const char* label, int16_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_S16, data);
}
inline bool escgInputNumber(const char* label, uint16_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_U16, data);
}
inline bool escgInputNumber(const char* label, int32_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_S32, data);
}
inline bool escgInputNumber(const char* label, uint32_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_U32, data);
}
inline bool escgInputNumber(const char* label, int64_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_S64, data);
}
inline bool escgInputNumber(const char* label, uint64_t* data) {
	return ImGui::InputScalar(label, ImGuiDataType_U64, data);
}
inline bool escgInputNumber(const char* label, float* data) {
	return ImGui::InputScalar(label, ImGuiDataType_Float, data);
}
inline bool escgInputNumber(const char* label, double* data) {
	return ImGui::InputScalar(label, ImGuiDataType_Double, data);
}
inline bool escgInputSlider(const char* label, int8_t* data, int8_t min, int8_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_S8, data, &min, &max, "%d", flags);
}
inline bool escgInputSlider(const char* label, uint8_t* data, uint8_t min, uint8_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_U8, data, &min, &max, "%u", flags);
}
inline bool escgInputSlider(const char* label, int16_t* data, int16_t min, int16_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_S16, data, &min, &max, "%d", flags);
}
inline bool escgInputSlider(const char* label, uint16_t* data, uint16_t min, uint16_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_U16, data, &min, &max, "%u", flags);
}
inline bool escgInputSlider(const char* label, int32_t* data, int32_t min, int32_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_S32, data, &min, &max, "%d", flags);
}
inline bool escgInputSlider(const char* label, uint32_t* data, uint32_t min, uint32_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_U32, data, &min, &max, "%u", flags);
}
inline bool escgInputSlider(const char* label, int64_t* data, int64_t min, int64_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_S64, data, &min, &max, PRId64, flags);
}
inline bool escgInputSlider(const char* label, uint64_t* data, uint64_t min, uint64_t max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_U64, data, &min, &max, PRIu64, flags);
}
inline bool escgInputSlider(const char* label, float* data, float min, float max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_Float, data, &min, &max, "%.2f", flags);
}
inline bool escgInputSlider(const char* label, double* data, double min, double max, ImGuiSliderFlags flags) {
	return ImGui::SliderScalar(label, ImGuiDataType_Double, data, &min, &max, "%.2f", flags);
}
// I honestly do not get the reasoning for
// not having an InputText overload that supports std::string directly in imgui.
// Every comment beats around the bush telling you it is possible, but info on how is elsewhere

// Code borrowed from imgui extras to save you a massive hastle
// Alternatively you can copy imgui_stdlib.cpp and imgui_stdlib.h from <imgui_directory>/misc/cpp/ to <imgui_directory>
// and have an `// include: <imgui_stdlib.h>` in the input file(or shove the include in this file)
// It'll probably come in handy for you if you're doing your own text inputs elsewhere, so at least consider it.
// It's probably also a good idea to do either both or neither to avoid any potential symbol redefinition issues.
#ifndef ESCG_IMGUI_STDLIB

struct InputTextCallback_UserData
{
    std::string*            Str;
    ImGuiInputTextCallback  ChainCallback;
    void*                   ChainCallbackUserData;
};

static int InputTextCallback(ImGuiInputTextCallbackData* data)
{
    InputTextCallback_UserData* user_data = (InputTextCallback_UserData*)data->UserData;
    if (data->EventFlag == ImGuiInputTextFlags_CallbackResize)
    {
        // Resize string callback
        // If for some reason we refuse the new length (BufTextLen) and/or capacity (BufSize) we need to set them back to what we want.
        std::string* str = user_data->Str;
        IM_ASSERT(data->Buf == str->c_str());
        str->resize(data->BufTextLen);
        data->Buf = (char*)str->c_str();
    }
    else if (user_data->ChainCallback)
    {
        // Forward to user callback, if any
        data->UserData = user_data->ChainCallbackUserData;
        return user_data->ChainCallback(data);
    }
    return 0;
}

namespace ImGui
{
	bool InputText(const char* label, std::string* str, ImGuiInputTextFlags flags = 0, ImGuiInputTextCallback callback = NULL, void* user_data = NULL)
	{
		IM_ASSERT((flags & ImGuiInputTextFlags_CallbackResize) == 0);
		flags |= ImGuiInputTextFlags_CallbackResize;

		InputTextCallback_UserData cb_user_data;
		cb_user_data.Str = str;
		cb_user_data.ChainCallback = callback;
		cb_user_data.ChainCallbackUserData = user_data;
		return InputText(label, (char*) str->c_str(), str->capacity() + 1, flags, InputTextCallback, &cb_user_data);
	}
}

#endif

void escgShowUI(ESCG_MAIN_TYPE& obj, bool* open = nullptr) {
	if(!ImGui::Begin("Settings", open)) {
		ImGui::End();
		return;
	}
#pragma escg(imgui)
	ImGui::End();
}

#endif /* ESCG_IMGUI */

nlohmann::json escgToJson(const ESCG_MAIN_TYPE& obj) {
	nlohmann::json ret {
#pragma escg(serialize)
	};
	return ret;
}

void escgWriteToFile(const std::filesystem::path& path, const ESCG_MAIN_TYPE& obj) {
	nlohmann::json data = escgToJson(obj);
	std::ofstream stream(path);
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