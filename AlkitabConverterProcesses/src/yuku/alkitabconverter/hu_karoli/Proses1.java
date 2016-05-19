package yuku.alkitabconverter.hu_karoli;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import yuku.alkitabconverter.yes1.Yes1File;
import yuku.alkitabconverter.yes1.Yes1File.InfoEdisi;
import yuku.alkitabconverter.yes1.Yes1File.InfoKitab;
import yuku.alkitabconverter.yes1.Yes1File.Teks;
import yuku.alkitabconverter.util.Hitungan31102;
import yuku.alkitabconverter.util.Rec;
import yuku.alkitabconverter.util.RecUtil;
import yuku.alkitabconverter.yes_common.Yes1Common;

public class Proses1 {
	static String INPUT_TEKS_1 = "./bahan/hu-karoli/in/hu-karoli-bare.txt";
	public static String INPUT_TEKS_ENCODING = "utf-8";
	public static int INPUT_TEKS_ENCODING_YES = 2; // 1: ascii; 2: utf-8;
	public static String INPUT_KITAB = "./bahan/hu-karoli/in/hu-karoli-kitab.txt";
	static String OUTPUT_YES = "./bahan/hu-karoli/out/hu-karoli.yes";
	public static int OUTPUT_ADA_PERIKOP = 0;
	static String INFO_NAMA = "hu-karoli";
	static String INFO_JUDUL = "Károli";
	static String INFO_KETERANGAN = "Hungarian Vizsoly Biblia translated by Károli Gáspár (1590)";

	List<Rec> xrec = new ArrayList<>();

	public static void main(String[] args) throws Exception {
		new Proses1().u();
	}

	private void u() throws Exception {
		Scanner sc = new Scanner(new File(INPUT_TEKS_1), INPUT_TEKS_ENCODING);
		
		List<Rec> xrec = new ArrayList<>();
		int offset_0 = 0;
		
		while (sc.hasNextLine()) {
			String line = sc.nextLine();
			line = line.trim();
			
			// ayat
			Rec rec = new Rec();
			rec.book_1 = Hitungan31102.kitab_1(offset_0);
			rec.chapter_1 = Hitungan31102.pasal_1(offset_0);
			rec.verse_1 = Hitungan31102.ayat_1(offset_0);
			rec.text = line;
			
			xrec.add(rec);
			offset_0++;
		}
		
		System.out.println("Total verses: " + xrec.size());

		////////// PROSES KE YES

		final InfoEdisi infoEdisi = Yes1Common.infoEdisi(INFO_NAMA, null, INFO_JUDUL, RecUtil.hitungKitab(xrec), OUTPUT_ADA_PERIKOP, INFO_KETERANGAN, INPUT_TEKS_ENCODING_YES, null);
		final InfoKitab infoKitab = Yes1Common.infoKitab(xrec, INPUT_KITAB, INPUT_TEKS_ENCODING, INPUT_TEKS_ENCODING_YES);
		final Teks teks = Yes1Common.teks(xrec, INPUT_TEKS_ENCODING);
		
		Yes1File file = Yes1Common.bikinYesFile(infoEdisi, infoKitab, teks);
		
		file.output(new RandomAccessFile(OUTPUT_YES, "rw"));
	}
}
