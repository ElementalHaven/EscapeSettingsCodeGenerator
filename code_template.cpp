/************************************************
 * THIS FILE IS MACHINE GENERATED. DO NOT EDIT. *
 ************************************************/
#include <map>
#include <string>
#include <inttypes.h>
#pragma escg(includes)

#define COL_SCALE float(UINT8_MAX)
// if you're using MSVC, you have to go to Config -> C/C++ -> Command Line
// and add "/Zc:__cplusplus" to additional options
// or else __cplusplus will always be 197711L
#if __cplusplus >= 201103L || (defined(_MSC_VER) && _MSC_VER >= 1900)
#define ESCG_CONSTEXPR constexpr
#else
#define ESCG_CONSTEXPR const
#endif

static std::string escgToColorString(int32_t col) {
	static char buf[8];
	sprintf_s(buf, "#%06X", col);
	return buf;
}
static std::string escgToColorString(uint32_t col) {
	static char buf[8];
	sprintf_s(buf, "#%06X", col);
	return buf;
}
static std::string escgToColorString(float* col) {
	uint32_t rgb = (static_cast<uint32_t>(col[0] * UINT8_MAX) << 16) |
		(static_cast<uint32_t>(col[1] * UINT8_MAX) << 8) |
		static_cast<uint32_t>(col[2] * UINT8_MAX);
	return escgToColorString(rgb);
}
#ifdef GLM_PLATFORM
inline std::string escgToColorString(glm::vec3& col) {
	return escgToColorString(&col.r);
}
inline std::string escgToColorString(glm::vec4& col) {
	return escgToColorString(&col.r);
}
#endif
static void escgFromColorString(const std::string& strIn, int32_t& colOut) {
	if(strIn.length() != 7) return;
	colOut = std::stol(strIn.c_str() + 1, nullptr, 16);
}
static void escgFromColorString(const std::string& strIn, uint32_t& colOut) {
	if(strIn.length() != 7) return;
	colOut = std::stol(strIn.c_str() + 1, nullptr, 16) | 0xFF000000u;
}
static void escgFromColorString(const std::string& strIn, float* colOut) {
	union {
		uint32_t tmp;
		uint8_t rgb[3];
	};
	escgFromColorString(strIn, tmp);
	colOut[2] = rgb[0] / COL_SCALE;
	colOut[1] = rgb[1] / COL_SCALE;
	colOut[0] = rgb[2] / COL_SCALE;
	// not touching alpha
}
#ifdef GLM_PLATFORM
inline void escgFromColorString(const std::string& strIn, glm::vec3& colOut) {
	escgFromColorString(strIn, &colOut.r);
}
inline void escgFromColorString(const std::string& strIn, glm::vec4& colOut) {
	escgFromColorString(strIn, &colOut.r);
}
#endif

#pragma escg(enums)

#pragma escg(deserialize)

// ESCG_IMGUI is automatically defined in the includes section
// if imgui is detected as being included
// do not define it manually
#ifdef ESCG_IMGUI

// These exist to provide an overloaded method for numeric inputs.
// rather than having the generator track the exact numeric type,
// it becomes the C++ compiler's job to do that instead
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
static bool escgInputColor(const char* label, uint8_t data[4]) {
	float rgb[3];
	rgb[0] = data[2] / COL_SCALE;
	rgb[1] = data[1] / COL_SCALE;
	rgb[2] = data[0] / COL_SCALE;
	// no alpha support
	bool ret = ImGui::ColorEdit3(label, rgb);
	if(ret) {
		data[2] = static_cast<uint8_t>(rgb[0] * COL_SCALE);
		data[1] = static_cast<uint8_t>(rgb[1] * COL_SCALE);
		data[0] = static_cast<uint8_t>(rgb[2] * COL_SCALE);
	}
	return ret;
}
inline bool escgInputColor(const char* label, uint32_t* data) {
	return escgInputColor(label, reinterpret_cast<uint8_t*>(data));
}
inline bool escgInputColor(const char* label, int32_t* data) {
	return escgInputColor(label, reinterpret_cast<uint8_t*>(data));
}
#ifdef GLM_PLATFORM
inline bool escgInputColor(const char* label, glm::vec3* data) {
	// Would be more appropriate to use glm::value_ptr
	// however we already have the type as a pointer.
	// I almost just shoved in a reinterpret_cast.
	// The below should likely result in identical machine code as that
	return ImGui::ColorEdit3(label, &data->r);
}
inline bool escgInputColor(const char* label, glm::vec4* data, bool alpha = true) {
	return ImGui::ColorEdit4(label, &data->r, alpha ? 0 : ImGuiColorEditFlags_NoAlpha);
}
#endif
static bool escgInputColor(const char* label, std::string* data) {
	float tmp[3];
	escgFromColorString(*data, tmp);
	bool ret = ImGui::ColorEdit3(label, tmp);
	if(ret) {
		*data = escgToColorString(tmp);
	}
	return ret;
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

#pragma escg(imgui)

#endif /* ESCG_IMGUI */

#pragma escg(serialize)