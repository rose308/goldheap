package yuku.alkitab.base.util;

import yuku.alkitab.base.App;

import java.text.DateFormat;
import java.util.Date;

public class Sqlitil {
	static ThreadLocal<DateFormat> mediumDateFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return android.text.format.DateFormat.getMediumDateFormat(App.context);
		}
	};

	static ThreadLocal<DateFormat> timeFormat = new ThreadLocal<DateFormat>() {
		@Override
		protected DateFormat initialValue() {
			return android.text.format.DateFormat.getTimeFormat(App.context);
		}
	};

	public static int nowDateTime() {
		return (int) (System.currentTimeMillis() / 1000);
	}

	/** Convert Date to unix time */
	public static int toInt(Date date) {
		return (int) (date.getTime() / 1000);
	}

	/** Convert unix time to Date */
	public static Date toDate(int date) {
		return new Date((long)date * 1000);
	}
	
	public static String toLocaleDateMedium(Date date) {
		return mediumDateFormat.get().format(date);
	}

	public static String toLocaleTime(Date date) {
		return timeFormat.get().format(date);
	}
}
