package yuku.alkitabconverter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import yuku.bintex.BintexWriter;

public class KonvertPerikop {
	static String nama;
	
	public static void main(String[] args) throws Exception {
		nama = args[0];
		
		new KonvertPerikop().convert("../Alkitab/publikasi/" + nama + "_perikop_1.txt", "../Alkitab/publikasi/" + nama + "_raw/" + nama + "_perikop_blok_bt.bt", "../Alkitab/publikasi/" + nama + "_raw/"  + nama + "_perikop_index_bt.bt");
	}
	
	int bolong = 0;

	private void convert(String nfi, String nfo, String nfindex) throws Exception {
		Scanner sc = new Scanner(new File(nfi));
		
		BintexWriter out = new BintexWriter(new FileOutputStream(nfo));
		BintexWriter index = new BintexWriter(new FileOutputStream(nfindex));

		String judul = null;
		String perikop = null;
		ArrayList<String> xparalel = new ArrayList<>();
		ArrayList<Integer> xofset = new ArrayList<>();
		ArrayList<Integer> xari = new ArrayList<>();
		
		while (sc.hasNextLine()) {
			String line = sc.nextLine().trim();
			String isi;
			int jenis;
			
			if (line.startsWith("Judul: ")) {
				jenis = 1;
				isi = line.substring(7);
			} else if (line.startsWith("Perikop: ")) {
				jenis = 2;
				isi = line.substring(9);
			} else if (line.startsWith("Paralel: ")) {
				jenis = 3;
				isi = line.substring(9);
			} else {
				throw new RuntimeException("baris = " + line);
			}
			
			if (jenis == 1) {
				// cek sebelumnya ada ga
				if (judul != null) {
					u(xofset, xari, out, index, perikop);
					t(out, judul, xparalel);
					
					judul = null;
					perikop = null;
					xparalel.clear();
				}
				
				judul = isi;
			} else if (jenis == 2) {
				if (perikop != null) throw new RuntimeException("harusnya perikop 1 aja");
				perikop = isi;
			} else if (jenis == 3) {
				xparalel.add(isi);
			}
		}
		// terakhir!
		u(xofset, xari, out, index, perikop);
		t(out, judul, xparalel);
		
		if (xofset.size() == xari.size()) {
			index.writeInt(xofset.size()); // int adaBerapaEntri
			for (int i = 0; i < xofset.size(); i++) {
				index.writeInt(xari.get(i)); // ari untuk entri ini
				index.writeInt(xofset.get(i)); // ofset ke blok untuk entri ini
			}
		} else {
			throw new RuntimeException("xofset.size() != xari.size()");
		}
		
		out.close();
		index.close();
	}
	static int lastAri = 0;

	private void u(ArrayList<Integer> xofset, ArrayList<Integer> xari, BintexWriter out, BintexWriter index, String perikop) throws IOException {
		int ofset = out.getPos();
		
		if (perikop == null) {
			bolong++;
			
			xofset.add(ofset);
		} else {
			if (perikop.contains(";")) {
				String[] bag = perikop.split(";");
				System.out.println("Perikop " + perikop + " disederhanakan jadi: " + bag[0]);
				perikop = bag[0];
			}
			
			String[] s1 = perikop.split(" +");
			if (s1.length != 2) {
				throw new RuntimeException("perikop = " + perikop);
			}
			
			int kitab = n2a(s1[0]);
			
			String pa;
			if (s1[1].contains("-")) {
				pa = s1[1].substring(0, s1[1].indexOf("-"));
			} else {
				pa = s1[1];
			}
			
			String[] s2 = pa.split(":");
			if (s2.length != 2 && s2.length != 1) {
				throw new RuntimeException("pa = " + pa + "; perikop = " + perikop);
			}
			
			int pasal = Integer.parseInt(s2[0]);
			int ayat = s2.length == 1? 1: Integer.parseInt(s2[1]);
			
			System.out.println("u: " + perikop + " -> " + kitab + " " + pasal + " " + ayat);
			
			int ari = kitab << 16 | pasal << 8 | ayat;
			
			if (ari < lastAri) {
				throw new RuntimeException("ari sekarang: " + ari + " lastAri: " + lastAri);
			}
			lastAri = ari;
			
			xari.add(ari);
			
			for (int i = 0; i < bolong; i++) {
				xari.add(ari);
			}
			bolong = 0;
			
			xofset.add(ofset);
		}
	}

	private void t(BintexWriter writer, String judul, ArrayList<String> xparalel) throws IOException {
		// tulis versi
		writer.writeUint8(1);
		// tulis judul
		writer.writeShortString(judul);
		// tulis nparalel
		writer.writeUint8(xparalel.size());
		// tulis xparalel
		for (String paralel: xparalel) {
			writer.writeShortString(paralel);
		}
	}
	
	private static int n2a(String n) {
		for (int i = 0; i < nk.length; i++) {
			if (n.equals(nk[i])) {
				return i;
			}
		}
		throw new RuntimeException("n = " + n);
	}
	
	static String[] nk = { "Kej", "Kel",
		"Im", "Bil", "Ul", "Yos",
		"Hak", "Rut", "1Sam", "2Sam", "1Raj", "2Raj", "1Taw", "2Taw", "Ezr",
		"Neh", "Est", "Ayub", "Mazm", "Ams", "Pengkh", "Kid",
		"Yes", "Yer", "Rat", "Yeh", "Dan", "Hos", "Yoel", "Am", "Ob", "Yun",
		"Mi", "Nah", "Hab", "Zef", "Hag", "Za", "Mal",
		"Mat", "Mrk", "Luk", "Yoh", "Kis", "Rom", "1Kor", "2Kor", "Gal", "Ef", "Fili", "Kol",
		"1Tes", "2Tes", "1Tim", "2Tim", "Tit",
		"Filem", "Ibr", "Yak", "1Pet", "2Pet", "1Yoh", "2Yoh", "3Yoh", "Yud",
		"Wahy",
	};
}
