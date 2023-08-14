package escg;

// Despite the documentation on UiTypes for Groups listed here
// There is currently very little control for them
public enum UIType {
	DEFAULT,
	/** Not shown in the UI */
	NONE,
	/** A textbox. the default for strings */
	TEXT,
	/** Just a textbox because I thought there was a specialized type when there wasnt */
	FILE,
	/**
	 * A number with increment and decrement buttons.
	 * Only usable by numeric settings and the default for them
	 */
	NUMBER,
	/** A slider. only usable by numeric settings with a min and max and the default for them */
	SLIDER,
	/** A logarithmically-scaled slider. only usable by numeric settings with a min and max */
	LOGSLIDER,
	/** A checkbox. Only usable by boolean settings and the default for them */
	CHECKBOX,
	/**
	 * A section confined to a tab. usable only by groups.
	 * The default for groups directly contained by the root object
	 */
	TAB,
	/** A section with a large header. usable only by groups */
	GROUP,
	/** A headerless section. usable only by groups and the default for them */
	TREE,
	/** A dropdown menu. usable only by enum settings and the default for them */
	COMBOBOX,
	CUSTOM;
	
	public static UIType getType(Setting setting) {
		UIType wanted = setting.uiType;
		if(wanted == null || !wanted.isUsableBy(setting)) wanted = DEFAULT;
		if(wanted != DEFAULT) return wanted;
		
		Class<?> cls = setting.javaType;
		if(Number.class.isAssignableFrom(cls)) {
			if(setting.minValue != null && setting.maxValue != null) {
				return SLIDER;
			}
			return NUMBER;
		}
		if(cls == String.class) return TEXT;
		if(cls == Boolean.class) return CHECKBOX;
		if(cls == Enum.class) return COMBOBOX;
		// not sure how we got here if we did
		return NONE;
	}
	
	public boolean isUsableBy(SettingOrGroup item) {
		if(this == NONE || this == DEFAULT || this == CUSTOM) return true;
		
		if(item.getClass() == Group.class) {
			return isUsableBy((Group) item);
		}
		return isUsableBy((Setting) item);
	}
	
	public boolean isUsableBy(Setting setting) {
		switch(this) {
			case NONE:
			case DEFAULT:
			case CUSTOM:
				return true;
			case TEXT:
				return setting.javaType == String.class;
			case FILE:
				return setting.javaType == String.class;
			case CHECKBOX:
				return setting.javaType == Boolean.class;
			case NUMBER:
				return Number.class.isAssignableFrom(setting.javaType);
			case SLIDER:
			case LOGSLIDER:
				return setting.maxValue != null && setting.minValue != null &&
					Number.class.isAssignableFrom(setting.javaType);
			case COMBOBOX:
				return setting.javaType == Enum.class;
			default:
				return false;
		}
	}
	
	public boolean isUsableBy(Group group) {
		switch(this) {
			case NONE:
			case DEFAULT:
			case TAB:
			case GROUP:
			case TREE:
			case CUSTOM:
				return true;
			default:
				return false;
		}
	}
}