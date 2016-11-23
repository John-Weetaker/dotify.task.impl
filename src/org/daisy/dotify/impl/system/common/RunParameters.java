package org.daisy.dotify.impl.system.common;

import java.util.HashMap;
import java.util.Map;

/**
 * Provides parameters needed when running a Task system.
 * NOTE: some default properties are set in this class.
 * This behavior is misplaced and will be removed in future
 * versions. Users of this class are advised not to rely 
 * on the existence of these properties.
 * 
 * @author Joel Håkansson
 */
public class RunParameters {
	public static String COLS = "cols";
	public static String PAGE_WIDTH = "page-width";
	public static String INNER_MARGIN = "inner-margin";
	public static String OUTER_MARGIN = "outer-margin";
	public static String ROWS = "rows";
	public static String PAGE_HEIGHT = "page-height";
	public static String ROWGAP = "rowgap";
	public static String ROW_SPACING = "row-spacing";

	public static Map<String, Object> fromMap(Map<String, Object> map) {
		Map<String, Object> p1 = new HashMap<>();
		p1.putAll(map);
		verifyAndSetWidth(p1);
		verifyAndSetHeight(p1);
		verifyAndSetRowSpacing(p1);
		return p1;
	}
	
	static void verifyAndSetWidth(Map<String, Object> p1) {
		Integer cols = asInteger(p1.get(COLS), null);
		Integer pageWidth = asInteger(p1.get(PAGE_WIDTH), null);
		int innerMargin = asInteger(p1.get(INNER_MARGIN), 5);
		int outerMargin = asInteger(p1.get(OUTER_MARGIN), 2);

		if (cols==null && pageWidth==null) {
			//use default
			cols = 28;
		}
		if (cols==null) {
			cols = pageWidth - innerMargin - outerMargin;
		} else if (pageWidth==null) {
			pageWidth = innerMargin + outerMargin + cols;
		} else if (pageWidth!=innerMargin + outerMargin + cols) {
			throw new IllegalArgumentException("Conflicting definitions: " + PAGE_WIDTH + "/" + COLS + "/" + INNER_MARGIN + "/" + OUTER_MARGIN);
		}

		p1.put(COLS, cols);
		p1.put(PAGE_WIDTH, pageWidth);
		p1.put(INNER_MARGIN, innerMargin);
		p1.put(OUTER_MARGIN, outerMargin);
	}
	
	static void verifyAndSetHeight(Map<String, Object> p1) {
		Integer rows = asInteger(p1.get(ROWS), null);
		Integer pageHeight = asInteger(p1.get(PAGE_HEIGHT), null);
		if (rows==null && pageHeight==null) {
			//use default
			rows = 29;
		}
		if (rows==null) {
			rows = pageHeight;
		} else if (pageHeight==null) {
			pageHeight = rows;
		} else if (pageHeight!=rows) {
			throw new IllegalArgumentException("Conflicting definitions: " + PAGE_HEIGHT + "/" + ROWS);
		}
		p1.put(ROWS, rows);
		p1.put(PAGE_HEIGHT, pageHeight);
	}
	
	static void verifyAndSetRowSpacing(Map<String, Object> p1) {
		Integer rowgap = asInteger(p1.get(ROWGAP), null);
		Float rowSpacing = asFloat(p1.get(ROW_SPACING), null);
		if (rowgap==null && rowSpacing==null) {
			//use default
			rowgap = 0;
		}
		if (rowgap==null) {
			float t = (rowSpacing-1)*4;
			if (t<0) {
				throw new IllegalArgumentException("Negative " + ROWGAP + " caused by the value of " + ROW_SPACING + ":" + rowSpacing);
			}
			rowgap = Math.round(t);
		} else if (rowSpacing==null) {
			rowSpacing = (rowgap/4f)+1;
		} else if (!rowSpacing.equals((rowgap/4f)+1)) {
			throw new IllegalArgumentException("Conflicting definitions: " + ROWGAP + "/" + ROW_SPACING);
		}
		p1.put(ROWGAP, rowgap);
		p1.put(ROW_SPACING, rowSpacing);
	}
	
	private static Integer asInteger(Object o, Integer def) {
		if (o instanceof Integer) {
			return (Integer)o;
		} else if (o instanceof String) {
			try {
				return Integer.parseInt((String)o);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}
	
	private static Float asFloat(Object o, Float def) {
		if (o instanceof Float) {
			return (Float)o;
		} else if (o instanceof String) {
			try {
				return Float.parseFloat((String)o);
			} catch (NumberFormatException e) {
				return def;
			}
		}
		return def;
	}
}
