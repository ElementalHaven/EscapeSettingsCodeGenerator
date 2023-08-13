package escg;

public class Setting extends SettingOrGroup {
	/**
	 * Autodetermined from the C++ type in the member declaration<br>
	 * Used to decide the appropriate type of functions to use for validation.
	 */
	public Class<?>	javaType;
	/**
	 * C++ type recorded from the member declaration<br>
	 * Used for the creation of parameters and temporary variables
	 * for tracking and validating the value.
	 */
	public String	cppType;
	/**
	 * Specified with the optional parameter {@code min:}<br>
	 * The minimum allowable value for a numeric type.
	 */
	public String	minValue;
	/**
	 * Specified with the optional parameter {@code max:}<br>
	 * The maximum allowable value for a numeric type.
	 */
	public String	maxValue;
	/**
	 * Specified by the value following the equals
	 * sign for the member declaration<br>
	 * The value that is used if none is specified in the config file.
	 */
	public String	defaultValue;
	/**
	 * Specified with the optional parameter {@code special:}<br>
	 * A value that exists outside the min and max range
	 * and is used to denote a special case, such as autodetect.
	 */
	public String	specialValue;
	/**
	 * The name of a function that is called on the value
	 * after bounds checking is done
	 * to ensure that the value meets additional requirements.
	 * It may also be used to calculate derived values
	 * or to send the values to some API to be applied.<br>
	 * function syntax is currently to be decided.
	 */
	public String	validatorFunc;
	
	public void clearInfo() {
		javaType = null;
		minValue = null;
		maxValue = null;
		defaultValue = null;
		specialValue = null;
		validatorFunc = null;
		
		codeName = null;
		jsonName = null;
		friendlyName = null;
		description = null;
		uiType = UIType.DEFAULT;

		// unknown how to handle exclude
		// if set with a tag, it should revert to protection level-based value
	}
	
	@Override
	public void copyInfo(Setting toCopy) {
		super.copyInfo(toCopy);
		
		javaType = toCopy.javaType;
		cppType = toCopy.cppType;
		
		minValue = toCopy.minValue;
		maxValue = toCopy.maxValue;
		defaultValue = toCopy.defaultValue;
		specialValue = toCopy.specialValue;
		validatorFunc = toCopy.validatorFunc;
	}
	
	public void writerReaderCode(CppWriter writer, String jsonName, String fullStructName) {
		writer.startLine();
		writer.append(fullStructName).append(" = ");
		if(javaType == Enum.class) {
			writer.append("escgEnumS2V_").append(cppType);
			writer.append('(').append(jsonName).append(".get<std::string>())");
			/* everything not an enum should be handlable by the template conversion itself
		} else if(javaType == String.class) {
			writer.append(jsonName).append(".get<std::string>()");
		} else if(javaType == Boolean.class) {
			writer.append(jsonName).append(".get<bool>()");
			//*/
		} else {
			writer.append(jsonName).append(".get<").append(cppType).append(">()");
		}
		writer.append(';').endLine();
	}
	
	public void setType(String cppType) {
		this.cppType = cppType;
		javaType = Utils.getJavaType(cppType);
		if(javaType == null) {
			if(CppEnum.BY_NAME.keySet().contains(cppType)) {
				javaType = Enum.class;
			}
		}
	}
}