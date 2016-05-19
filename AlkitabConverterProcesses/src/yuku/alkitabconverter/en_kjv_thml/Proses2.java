package yuku.alkitabconverter.en_kjv_thml;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;

public class Proses2 {
	PrintWriter pw;
	Map<String, Integer> nomerKitab;
	private int hitungan = 0;

	public static void main(String[] args) throws Exception {
		new Proses2().loadUpTheData();
	}
	
	public void loadUpTheData() throws Exception {
		muatNomerKitab();
		
		Scanner sc = new Scanner(new FileInputStream("./bahan/en-kjv-thml/in/kjv.proses"), "utf-8");
		pw = new PrintWriter(new File("./bahan/en-kjv-thml/in/kjv3_teks_bdb.txt"), "utf-8");
		
		proses(sc);
		pw.close();
		sc.close();
	}

	private void muatNomerKitab() throws Exception {
		Scanner sc = new Scanner(new FileInputStream("./bahan/en-kjv-thml/in/kjv.nomerkitab"), "utf-8");
		nomerKitab = new TreeMap<>();
		while (sc.hasNext()) {
			String k = sc.next();
			int v = sc.nextInt();
			if (v != -1) {
				nomerKitab.put(k, v);
			}
		}
		sc.close();
	}

	private void proses(Scanner sc) {
		String parsed = null;
		StringBuilder isi = new StringBuilder();
		
		while (sc.hasNextLine()) {
			String baris = sc.nextLine();
			if (parsed != null) {
				if (!baris.startsWith("#ayat:|")) {
					if (isi.length() != 0) {
						isi.append(' ');
					}
					isi.append(baris.trim());
				}
			}
			if (baris.startsWith("#ayat:|")) {
				// komit yang lama
				if (parsed != null) {
					tulis(parsed, isi);
				}
				parsed = baris.substring(7);
			}
		}

		tulis(parsed, isi);
	}

	private void tulis(String parsed, StringBuilder isi) {
		if (prosesParsed(parsed)) {
			output('\t');
			String isi2 = isi.toString().trim().replaceAll("\u2019", "'").replace("<i>", "@9").replace("</i>", "@7").replace("<red>", "@6").replace("</red>", "@5").replaceAll("\\s+@5$", "@5").replace("@6@5", "").replace("- ", "-");
			if (isi2.indexOf('@') != -1) {
				isi2 = "@@" + isi2;
			}
			output(isi2);
			output('\n');
		}
		isi.setLength(0);
	}

	private boolean prosesParsed(String parsed) {
		String[] xkolom = parsed.split("\\|");
		String n = xkolom[0];
		int p = Integer.parseInt(xkolom[1]);
		int a = Integer.parseInt(xkolom[2]);
		Integer nokitab = nomerKitab.get(n);
		
		if (nokitab != null) {
			hitungan ++;
			output(hitungan);
			output('\t');
			output(nokitab + 1);
			output('\t');
			output(p);
			output('\t');
			output(a);
			return true;
		}
		return false;
	}

	public void output(Object x) {
		pw.print(x);
	}
}
