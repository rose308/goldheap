package yuku.alkitab.base.util;

import yuku.alkitab.model.SongInfo;
import yuku.kpri.model.Lyric;
import yuku.kpri.model.Song;
import yuku.kpri.model.Verse;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SongFilter {
	public static final String TAG = SongFilter.class.getSimpleName();

	public static class CompiledFilter {
		Pattern[] ps;
	}
	
	public static CompiledFilter compileFilter(String filter_string) {
		CompiledFilter res = new CompiledFilter();

		if (filter_string == null || filter_string.trim().length() == 0) {
			res.ps = null;
		} else {
			String[] tokens = QueryTokenizer.tokenize(filter_string);
			Pattern[] ps = new Pattern[tokens.length];
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i];
				if (QueryTokenizer.isPlussedToken(token)) {
					ps[i] = Pattern.compile("\\b" + Pattern.quote(QueryTokenizer.tokenWithoutPlus(token)) + "\\b", Pattern.CASE_INSENSITIVE);
				} else {
					ps[i] = Pattern.compile(Pattern.quote(token), Pattern.CASE_INSENSITIVE);
				}
			}
			res.ps = ps;
		}
		
		return res;
	}
	
	public static List<SongInfo> filterSongInfosByString(List<SongInfo> songInfos, String filter_string) {
		List<SongInfo> res = new ArrayList<>();
		
		if (filter_string == null) {
			res.addAll(songInfos);
		} else {
			CompiledFilter cf = compileFilter(filter_string);
			
			for (SongInfo songInfo: songInfos) {
				if (match(songInfo, cf)) res.add(songInfo);
			}
		}
		
		return res;		
	}
	
	public static List<Song> filterSongsByString(List<Song> songs, String filter_string) {
		List<Song> res = new ArrayList<>();
		
		if (filter_string == null) {
			res.addAll(songs);
		} else {
			CompiledFilter cf = compileFilter(filter_string);
			
			for (Song song: songs) {
				if (match(song, cf)) res.add(song);
			}
		}
		
		return res;
	}
	
	public static boolean match(SongInfo song, CompiledFilter cf) {
		Pattern[] ps = cf.ps;
		if (ps == null) return true; // empty filter? consider it passes
		
		int matches = 0;
		for (final Pattern p : ps) {
			if (match(song, p)) matches++;
		}
		return matches == ps.length;
	}
	
	public static boolean match(Song song, CompiledFilter cf) {
		Pattern[] ps = cf.ps;
		if (ps == null) return true; // empty filter? consider it passes

		int matches = 0;
		for (final Pattern p : ps) {
			if (match(song, p)) matches++;
		}
		return matches == ps.length;
	}
	
	private static boolean match(SongInfo song, Pattern p) {
		Matcher m = p.matcher("");
		
		if (find(song.code, m)) return true;
		if (find(song.title, m)) return true;
		if (song.title_original != null && find(song.title_original, m)) return true;
		
		return false;
	}
	
	private static boolean match(Song song, Pattern p) {
		Matcher m = p.matcher("");
		
		if (find(song.code, m)) return true;
		if (find(song.title, m)) return true;
		if (song.title_original != null && find(song.title_original, m)) return true;
		if (song.authors_lyric != null) for (String author_lyric: song.authors_lyric) {
			if (find(author_lyric, m)) return true;
		}
		if (song.authors_music != null) for (String author_music: song.authors_music) {
			if (find(author_music, m)) return true;
		}
		if (song.tune != null && find(song.tune, m)) return true;

		for (Lyric lyric: song.lyrics) {
			for (Verse verse: lyric.verses) {
				for (String line: verse.lines) {
					if (find(line, m)) return true;
				}
			}
		}
		return false;
	}
	
	private static boolean find(CharSequence s, Matcher m) {
		m.reset(s);
		return m.find();
	}
}
