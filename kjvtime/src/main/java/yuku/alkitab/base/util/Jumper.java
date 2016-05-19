package yuku.alkitab.base.util;

import android.util.Log;
import yuku.alkitab.debug.BuildConfig;
import yuku.alkitab.model.Book;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Jumper {
	public static final String TAG = Jumper.class.getSimpleName();

	public interface Logger {
		void d(String msg);
	}

	// default logger
	private Logger logger = msg -> Log.d(TAG, msg);
	
	private String p_book;
	private int p_chapter;
	private int p_verse;

	/** The reference string is a verse range, with dash as delimiter */
	private boolean p_hasRange = false;
	
	/** If bookId found from OSIS book names, set this to other than -1 and this will be returned */
	private int p_bookIdFromOsis = -1;

	private boolean parseSucceeded = false;
	
	public static class BookRef {
		public String condensed;
		public int bookId;

		@Override public String toString() {
			return condensed + ":" + bookId;
		}
	}
	
	private static WeakHashMap<Book[], List<BookRef>> condensedCache = new WeakHashMap<>();

	/**
	 * Parse with default logger.
	 */
	public Jumper(String referenceToParse) {
		parseSucceeded = parse(referenceToParse);
	}

	/**
	 * Override default logger and parse, e.g. for unit testing, to prevent calling android.util.Log methods.
	 */
	public void setLogger(final Logger logger) {
		this.logger = logger;
	}

	public Jumper(String referenceToParse, Logger logger) {
		this.logger = logger;
		parseSucceeded = parse(referenceToParse);
	}

	/**
	 * Can't be parsed as a pure number. "4-5": true. "Hello": true. "123": false. "12b": false.
	 * This is not the opposite of isNumber.
	 */
	private static boolean isWord(String s) {
		char c = s.charAt(0);
		if (c < '0' || c > '9') return true;
		
		try {
			Integer.parseInt(s);
			return false;
		} catch (NumberFormatException e) {
			return true;
		}
	}

	/**
	 * @return true if s is a number, or s is a number followed by a single lowercase character 'a'-'z' inclusive.
	 * That is for handling verse parts like 12a or 15b.
	 */
	private static boolean isNumber(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			// try special case
			if (s.length() > 1 && s.charAt(s.length() - 1) >= 'a' && s.charAt(s.length() - 1) <= 'z') {
				try {
					Integer.parseInt(s.substring(0, s.length() - 1));
					return true;
				} catch (NumberFormatException e2) {
					return false;
				}
			}
			return false;
		}
	}

	/**
	 * @return true if s is a number.
	 */
	private static boolean isPureNumber(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * @return integer value of s if s is a number, or s is a number followed by a single lowercase character 'a'-'z' inclusive.
	 * That is for handling verse parts like 12a or 15b.
	 * Returns 0 when it's unable to parse.
	 */
	private static int numberize(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			// try special case
			if (s.length() > 1 && s.charAt(s.length() - 1) >= 'a' && s.charAt(s.length() - 1) <= 'z') {
				try {
					return Integer.parseInt(s.substring(0, s.length() - 1));
				} catch (NumberFormatException e2) {
					return 0;
				}
			}
			return 0;
		}
	}
	
	private boolean parse0(String reference) {
		reference = reference.trim();
		
		if (reference.length() == 0) {
			return false;
		}

		if (BuildConfig.DEBUG) logger.d("jumper stage 0: " + reference);
		
		//# STAGE 4: replace en-dash and em-dash to normal dash
		if (reference.contains("\u2013") || reference.contains("\u2014")) {
			reference = reference.replaceAll("[\u2013\u2014]", "-");

			if (BuildConfig.DEBUG) logger.d("jumper stage 4: " + reference);
		}
		
		//# STAGE 5: Remove spaces on the left and right of "-"
		if (reference.indexOf('-') >= 0) {
			reference = reference.replaceAll("\\s+-\\s+|\\s+-|-\\s+", "-");

			if (BuildConfig.DEBUG) logger.d("jumper stage 5: " + reference);
		}
		
		//# STAGE 7: Check whether this is in strict osis ID format.
		// This can be BookName.Chapter.Verse or BookName.Chapter
		// Or, either of the above separated by a '-'
		notosis: {
			if (reference.indexOf('.') < 0) {
				break notosis; // must contain a dot
			}
			
			String osisId;
			if (reference.indexOf('-') >= 0) { // optionally a '-'
				final String[] osisIds = reference.split("-");
				if (osisIds.length != 2) { 
					break notosis; // wrong format
				}
				osisId = osisIds[0];
				p_hasRange = true;
			} else {
				osisId = reference;
			}
			
			Pattern p = OsisBookNames.getBookNameWithChapterAndOptionalVersePattern();
			Matcher m = p.matcher(osisId);
			if (m.matches()) {
				if (BuildConfig.DEBUG) logger.d("jumper stage 7: ref matching osis pattern found: " + osisId);
				String osisBookName = m.group(1);
				String chapter_s = m.group(2);
				String verse_s = m.group(3);
				
				try {
					p_bookIdFromOsis = OsisBookNames.osisBookNameToBookId(osisBookName);
					p_chapter = Integer.parseInt(chapter_s);
					p_verse = (verse_s == null || verse_s.length() == 0)? 0: Integer.parseInt(verse_s);
				} catch (Exception e) {
					throw new RuntimeException("Should not happen. In jumper stage 7", e);
				}

				if (BuildConfig.DEBUG) logger.d("jumper stage 7: successfully parsed osis id: " + p_bookIdFromOsis + ' ' + p_chapter + ' ' + p_verse);
				return true;
			}
		}
		
		//# STAGE 10: Split based on SPACE, :, PERIOD, and whitespaces between -'s and numbers.
		//# Sample of wrong output: [Kisah, rasul34, 6-7, 8]
		//# Sample of right output: [Kisah, rasul34, 6, -, 7, 8]
		String[] parts = reference.split("((\\s|:|\\.)+|(?=[0-9])(?<=-)|(?=-)(?<=[0-9][a-z]?))");
		if (BuildConfig.DEBUG) logger.d("jumper stage 10: " + Arrays.toString(parts));

		//# STAGE 12: Remove string from empty parts
		{
			int hasEmpty = 0;
			for (String b: parts) {
				if (b.length() == 0) {
					hasEmpty++;
					break;
				}
			}
			if (hasEmpty > 0) {
				String[] partsWithoutEmpties = new String[parts.length - hasEmpty];
				int c = 0;
				for (String b: parts) {
					if (b.length() != 0) {
						partsWithoutEmpties[c++] = b;
					}
				}
				parts = partsWithoutEmpties;
			}
		}
		if (BuildConfig.DEBUG) logger.d("jumper stage 12: " + Arrays.toString(parts));
		
		if (parts.length == 0) {
			return false;
		}
		
		//# STAGE 20: Expand cases like Joh3 to Joh 3
		//# Sample output: [Kisah, rasul, 34, 6, -, 7, 8]
		{
			ArrayList<String> bel = new ArrayList<>();

			for (String b: parts) {
				if (isWord(b)) {
					String number = "";
					for (int i = b.length() - 1; i >= 0; i--) {
						char c = b.charAt(i);
						if (c >= '0' && c <= '9') {
							// found a digit
							number = c + number;
						} else {
							break;
						}
					}
					
					if (number.length() > 0) { // a number found behind a word
						bel.add(b.substring(0, b.length() - number.length()));
						bel.add(number);
					} else {
						bel.add(b);
					}
				} else {
					bel.add(b);
				}
			}
			
			parts = bel.toArray(parts);
		}
		if (BuildConfig.DEBUG) logger.d("jumper stage 20: " + Arrays.toString(parts));
		

		//# STAGE 25: Look for part that is "-", then remove from it to the end.
		{
			boolean hasDash = false;
			int at = -1;
			
			for (int i = 0; i < parts.length; i++) {
				if ("-".equals(parts[i]) || "--".equals(parts[i])) {
					hasDash = true;
					at = i;
					break;
				}
			}
			
			if (hasDash) {
				String[] bel = new String[at];
				System.arraycopy(parts, 0, bel, 0, at);
				parts = bel;

				p_hasRange = true;

				if (BuildConfig.DEBUG) logger.d("jumper stage 25: " + Arrays.toString(parts));
			}
		}
		
		//# STAGE 30: Morph something like "3" "john" to "3 john"
		{
			ArrayList<String> bel = new ArrayList<>();
			
			int startWord = 0;
			
			// see from the right which one is not a number. That is the start of book.
			for (int i = parts.length - 1; i >= 0; i--) {
				final String part = parts[i];

				if (!isNumber(part)) {
					// this and all earlier than this is the book.
					startWord = i;

					break;
				}

				if (i == 0) { // special case, probably the first part is something like "1j" or "1y" for 1 John.
					if (isWord(part)) {
						startWord = i;

						break;
					}

					if (BuildConfig.DEBUG) {
						logger.d("jumper stage 30: too much, how come there are more than 2 numbers: returning false");
					}
					return false;
				}
			}

			String s = null;
			for (int j = 0; j <= startWord; j++) {
				s = (s == null)? parts[j]: s + " " + parts[j];
			}
			
			bel.add(s);
			for (int j = startWord+1; j < parts.length; j++) {
				bel.add(parts[j]);
			}

			parts = bel.toArray(new String[bel.size()]);
		}
		if (BuildConfig.DEBUG) logger.d("jumper stage 30: " + Arrays.toString(parts));
		
		if (parts.length == 1) { // 1 part only
			// It means it can be CHAPTER or BOOK only
			if (isWord(parts[0])) { // it's a BOOK
				p_book = parts[0];
				return true;
			} else { // it's a CHAPTER
				p_chapter = numberize(parts[0]);
				return true;
			}
		}

		if (parts.length == 2) { // 2 parts
			// means it could be CHAPTER VERSE (in the same book)
			if (isPureNumber(parts[0]) && isNumber(parts[1])) {
				p_chapter = numberize(parts[0]);
				p_verse = numberize(parts[1]);
				return true;
			}

			// or BOOK CHAPTER
			if (isPureNumber(parts[1])) {
				p_book = parts[0];
				p_chapter = numberize(parts[1]);
				return true;
			}
			return false;
		}
		
		if (parts.length == 3) { // 3 parts
			// it means it must be BOOK CHAPTER VERSE. Could not be otherwise.
			p_book = parts[0];
			p_chapter = numberize(parts[1]);
			p_verse = numberize(parts[2]);
			return true;
		}
		
		return false;
	}
	
	private boolean parse(String alamat) {
		boolean res = parse0(alamat);

		if (BuildConfig.DEBUG) {
			logger.d("jumper after parse0: p_book=" + p_book + " p_chapter=" + p_chapter + " p_verse=" + p_verse);
		}

		return res;
	}

	public static List<BookRef> createBookCandidates(Book[] books) {
		String[] bookNames = new String[books.length];
		int[] bookIds = new int[books.length];
		for (int i = 0, booksLength = books.length; i < booksLength; i++) {
			final Book book = books[i];
			bookNames[i] = book.shortName;
			bookIds[i] = book.bookId;
		}
		return createBookCandidates(bookNames, bookIds);
	}

	static List<BookRef> createBookCandidates(String[] bookNames, int[] bookIds) {
		// create cache of condensed book titles where all spaces are stripped and lowercased and "1" becomes "I", "2" becomes "II" etc.
		final List<BookRef> res = new ArrayList<>();
		
		for (int i = 0, len = bookNames.length; i < len; i++) {
			String condensed = bookNames[i].replaceAll("(\\s|-|_)+", "").toLowerCase(Locale.getDefault());

			{
				BookRef ref = new BookRef();
				ref.condensed = condensed;
				ref.bookId = bookIds[i];
					
				res.add(ref);
			}
			
			if (condensed.contains("1") || condensed.contains("2") || condensed.contains("3")) {
				condensed = condensed.replace("1", "i").replace("2", "ii").replace("3", "iii");
				
				BookRef ref = new BookRef();
				ref.condensed = condensed;
				ref.bookId = bookIds[i];
				
				res.add(ref);
			}
		}
		
		return res;
	}
	
	private int guessBook(List<BookRef> refs) {
		if (p_book == null) {
			return -1;
		}
		
		int res = -1;
		
		// 0. clean up p_book
		p_book = p_book.replaceAll("(\\s|-|_)", "").toLowerCase(Locale.getDefault());
		if (BuildConfig.DEBUG) logger.d("guessBook phase 0: p_book = " + p_book);
		
		// 1. try to match wholly (e.g.: "genesis", "john")
		for (BookRef ref: refs) {
			if (ref.condensed.equals(p_book)) {
				if (BuildConfig.DEBUG) logger.d("guessBook phase 1 success: " + p_book);
				return ref.bookId;
			}
		}
		
		// 2. try to match by prefix. If there is only one match, success
		int pos_forLater = -1;
		{
			int passed = 0;
			for (BookRef ref: refs) {
				if (ref.condensed.startsWith(p_book)) {
					passed++;
					if (passed == 1) pos_forLater = ref.bookId;
				}
			}
			
			if (passed == 1) {
				if (BuildConfig.DEBUG) logger.d("guessBook phase 2 success: " + pos_forLater + " for " + p_book);
				return pos_forLater;
			} else {
				if (BuildConfig.DEBUG) logger.d("guessBook phase 2: passed=" + passed);
			}
		}
		
		// 3. String matching only when p_book is 2 letters or more
		if (p_book.length() >= 2) {
			int minScore = 99999999;
			int pos = -1;
			
			for (BookRef ref: refs) {
				int score = Levenshtein.distance(p_book, ref.condensed);
				if (p_book.charAt(0) != ref.condensed.charAt(0)) {
					score += 150; // approximately 1.5 times insertion cost
				}

				if (BuildConfig.DEBUG) {
					logger.d("guessBook phase 3: with " + ref + ", score " + score);
				}

				if (score < minScore) {
					minScore = score;
					pos = ref.bookId;
				}
			}
			
			if (pos != -1) {
				if (BuildConfig.DEBUG) logger.d("guessBook phase 3 success: " + pos + " with score " + minScore);
				return pos;
			}
		}
		
		// 7. Return the earlier match if there is more than one that passed phase 2.
		if (pos_forLater != -1) {
			if (BuildConfig.DEBUG) logger.d("guessBook phase 7 success: " + pos_forLater + " for " + p_book);
			return pos_forLater;
		}
		
		return res;
	}
	
	/**
	 * @return whether the parsing succeeded
	 */
	public boolean getParseSucceeded() {
		return parseSucceeded;
	}

	public String getUnparsedBook() {
		return p_book;
	}
	
	/**
	 * @param books list of books from which the looked for book is searched
	 * @return bookId of one of the books (or -1).
	 */
	public int getBookId(Book[] books) {
		if (p_bookIdFromOsis != -1) return p_bookIdFromOsis;
		
		List<BookRef> refs = condensedCache.get(books);
		if (refs == null) {
			String[] bookNames = new String[books.length];
			int[] bookIds = new int[books.length];
			
			for (int i = 0; i < books.length; i++) {
				bookNames[i] = books[i].shortName;
				bookIds[i] = books[i].bookId;
			}
			
			refs = createBookCandidates(bookNames, bookIds);
			condensedCache.put(books, refs);
			if (BuildConfig.DEBUG) logger.d("New condensedCache entry: " + refs);
		}
		
		return guessBook(refs);
	}
	
	/**
	 * Give list of (bookName, bookId) from which the looked for book is searched.
	 * @return bookId of one of the books (or -1).
	 */
	public int getBookId(String[] bookNames, int[] bookIds) {
		if (p_bookIdFromOsis != -1) return p_bookIdFromOsis;
		
		return guessBook(createBookCandidates(bookNames, bookIds));
	}
	
	public int getChapter() {
		return p_chapter;
	}
	
	public int getVerse() {
		return p_verse;
	}

	/** The reference string is a verse range, with dash as delimiter */
	public boolean getHasRange() {
		return p_hasRange;
	}
}
