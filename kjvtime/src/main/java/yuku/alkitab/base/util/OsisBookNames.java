package yuku.alkitab.base.util;

import gnu.trove.map.hash.TObjectIntHashMap;
import yuku.alkitab.util.Ari;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OsisBookNames {
	public static final String TAG = OsisBookNames.class.getSimpleName();
	
	private static TObjectIntHashMap<String> bookNameToBookIdMap;
	private static Pattern bookNamePattern;
	private static Pattern bookNameWithChapterAndOptionalVersePattern;
	

	static String[] names = {
		"Gen",
		"Exod",
		"Lev",
		"Num",
		"Deut",
		"Josh",
		"Judg",
		"Ruth",
		"1Sam",
		"2Sam",
		"1Kgs",
		"2Kgs",
		"1Chr",
		"2Chr",
		"Ezra",
		"Neh",
		"Esth",
		"Job",
		"Ps",
		"Prov",
		"Eccl",
		"Song",
		"Isa",
		"Jer",
		"Lam",
		"Ezek",
		"Dan",
		"Hos",
		"Joel",
		"Amos",
		"Obad",
		"Jonah",
		"Mic",
		"Nah",
		"Hab",
		"Zeph",
		"Hag",
		"Zech",
		"Mal",
		"Matt",
		"Mark",
		"Luke",
		"John",
		"Acts",
		"Rom",
		"1Cor",
		"2Cor",
		"Gal",
		"Eph",
		"Phil",
		"Col",
		"1Thess",
		"2Thess",
		"1Tim",
		"2Tim",
		"Titus",
		"Phlm",
		"Heb",
		"Jas",
		"1Pet",
		"2Pet",
		"1John",
		"2John",
		"3John",
		"Jude",
		"Rev",
	};
	
	static {
		bookNameToBookIdMap = new TObjectIntHashMap<>(250, 0.75f, -1);
		for (int i = 0; i < names.length; i++) {
			bookNameToBookIdMap.put(names[i], i);
		}
	}
	
	public static Pattern getBookNamePattern() {
		if (bookNamePattern == null) {
			StringBuilder sb = new StringBuilder(400);
			sb.append('(');
			for (int i = 0; i < names.length; i++) {
				if (i != 0) sb.append('|');
				sb.append(names[i]);
			}
			sb.append(')');
			
			bookNamePattern = Pattern.compile(sb.toString());
		}
		return bookNamePattern;
	}
	
	public static Pattern getBookNameWithChapterAndOptionalVersePattern() {
		if (bookNameWithChapterAndOptionalVersePattern == null) {
			StringBuilder sb = new StringBuilder(400);
			sb.append('(');
			for (int i = 0; i < names.length; i++) {
				if (i != 0) sb.append('|');
				sb.append(names[i]);
			}
			sb.append(')');
			
			sb.append("\\.([1-9][0-9]{0,2})(?:\\.([1-9][0-9]{0,2}))?");
			
			bookNameWithChapterAndOptionalVersePattern = Pattern.compile(sb.toString());
		}
		return bookNameWithChapterAndOptionalVersePattern;
	}
	
	/** 
	 * @param osisBookName OSIS Book Name (only OT and NT currently supported)
	 * @return 0 to 65 when OK, -1 when not found
	 */
	public static int osisBookNameToBookId(String osisBookName) {
		if (osisBookName == null) return -1;
		return bookNameToBookIdMap.get(osisBookName);
	}

	public static int osisToAri(final String osis) {
		final Matcher m = getBookNameWithChapterAndOptionalVersePattern().matcher(osis);
		if (m.matches()) {
			String osisBookName = m.group(1);
			String chapter_s = m.group(2);
			String verse_s = m.group(3);

			final int bookId = osisBookNameToBookId(osisBookName);
			final int chapter_1 = Integer.parseInt(chapter_s);
			final int verse_1 = (verse_s == null || verse_s.length() == 0)? 0: Integer.parseInt(verse_s);

			return Ari.encode(bookId, chapter_1, verse_1);
		}

		return 0;
	}

	public static String getBookName(final int bookId) {
		if (bookId < 0 || bookId >= names.length) {
			return null;
		}
		return names[bookId];
	}
}
