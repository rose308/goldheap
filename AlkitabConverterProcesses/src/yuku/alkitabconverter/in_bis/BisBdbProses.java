package yuku.alkitabconverter.in_bis;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import yuku.alkitabconverter.yes1.Yes1File;
import yuku.alkitabconverter.yes1.Yes1File.InfoEdisi;
import yuku.alkitabconverter.yes1.Yes1File.InfoKitab;
import yuku.alkitabconverter.yes1.Yes1File.Kitab;
import yuku.alkitabconverter.yes1.Yes1File.Teks;
import yuku.alkitabconverter.bdb.BdbProses;
import yuku.alkitabconverter.util.Rec;

public class BisBdbProses {
	private static final String BIS_TEKS_BDB = "../Alkitab/publikasi/bis_teks_bdb.txt";
	private static final String BIS_YES_OUTPUT = "../Alkitab/publikasi/bis.yes";

	public static void main(String[] args) throws Exception {
		final Charset ascii = Charset.forName("ascii");
		
		ArrayList<Rec> xrec = new BdbProses().parse(BIS_TEKS_BDB, "ascii");
		
		final InfoEdisi infoEdisi = bisInfoEdisi();
		final InfoKitab infoKitab = bisInfoKitab(xrec);
		final Teks teks = bisTeks(xrec);
		
		Yes1File file = new Yes1File() {{
			this.xseksi = new Seksi[] {
				new Seksi() {
					@Override
					public byte[] nama() {
						return "infoEdisi___".getBytes(ascii);
					}

					@Override
					public IsiSeksi isi() {
						return infoEdisi;
					}
				},
				new Seksi() {
					@Override
					public byte[] nama() {
						return "infoKitab___".getBytes(ascii);
					}

					@Override
					public IsiSeksi isi() {
						return infoKitab;
					}
				},
				new Seksi() {
					@Override
					public byte[] nama() {
						return "perikopIndex".getBytes(ascii);
					}
					
					@Override
					public IsiSeksi isi() {
						return new NemplokSeksi("../Alkitab/publikasi/bis_perikop_index_bt.bt");
					}
				},
				new Seksi() {
					@Override
					public byte[] nama() {
						return "perikopBlok_".getBytes(ascii);
					}
					
					@Override
					public IsiSeksi isi() {
						return new NemplokSeksi("../Alkitab/publikasi/bis_perikop_blok_bt.bt");
					}
				},
				new Seksi() {
					@Override
					public byte[] nama() {
						return "teks________".getBytes(ascii);
					}

					@Override
					public IsiSeksi isi() {
						return teks;
					}
				}
			};
		}};
		
		file.output(new RandomAccessFile(BIS_YES_OUTPUT, "rw"));
	}


	private static Teks bisTeks(ArrayList<Rec> xrec) {
		final ArrayList<String> ss = new ArrayList<>();
		for (Rec rec: xrec) {
			ss.add(rec.text);
		}
		
		return new Teks("ascii") {{
			xisi = ss.toArray(new String[ss.size()]);
		}};
	}

	private static InfoEdisi bisInfoEdisi() {
		return new InfoEdisi() {{
			versi = 1;
			nama = "bis";
			shortName = "BIS";
			longName = "Bahasa Indonesia Sehari-hari";
			nkitab = 66;
			perikopAda = 1;
		}};
	}

	private static InfoKitab bisInfoKitab(ArrayList<Rec> xrec) throws Exception {
		final Kitab[] xkitab_ = new Kitab[66];
		
		String[] xjudul, xnama;
		xjudul = new String[66];
		xnama = new String[66];
		int p = 0;
		
		Scanner sc = new Scanner(new File("../Alkitab/publikasi/bis_kitab.txt"));
		while (sc.hasNextLine()) {
			String judul = sc.nextLine().trim();
			if (judul.length() > 0) {
				xjudul[p] = judul;
				xnama[p] = judul.replaceAll(" ", "_");
				p++;
			}
		}
		sc.close();
		
		int offsetTotal = 0;
		int offsetLewat = 0;
		int maxpasal_1 = 1;
		int lastpasal_1 = 1;
		int[] xnayat = new int[256];
		int[] xpasal_offset = new int[256];
		
		for (int kitabPos = 0; kitabPos < 66; kitabPos++) {
			xpasal_offset[0] = 0;
			
			for (Rec rec: xrec) {
				if (kitabPos + 1 == rec.book_1) {
					xnayat[rec.chapter_1 - 1]++;
					
					if (rec.chapter_1 > maxpasal_1) {
						maxpasal_1 = rec.chapter_1;
					}
					
					if (rec.chapter_1 != lastpasal_1) {
						xpasal_offset[lastpasal_1] = offsetLewat;
						lastpasal_1 = rec.chapter_1;
					}
					
					offsetLewat += rec.text.length() + 1; // tambah 1 karena '\n' nya
				}
			}
			xpasal_offset[maxpasal_1] = offsetLewat;
			
			Kitab kitab = new Kitab();
			kitab.versi = 1;
			kitab.pos = kitabPos;
			kitab.nama = xnama[kitabPos];
			kitab.judul = xjudul[kitabPos];
			kitab.npasal = maxpasal_1;
			kitab.nayat = new int[kitab.npasal];
			System.arraycopy(xnayat, 0, kitab.nayat, 0, kitab.npasal);
			System.out.println("kitab " + kitab.judul + " nayat: " + Arrays.toString(kitab.nayat));
			kitab.ayatLoncat = 0;
			kitab.pasal_offset = new int[kitab.npasal + 1];
			System.arraycopy(xpasal_offset, 0, kitab.pasal_offset, 0, kitab.npasal+1);
			System.out.println("kitab " + kitab.judul + " pasal_offset: " + Arrays.toString(kitab.pasal_offset));
			kitab.encoding = 1;
			kitab.offset = offsetTotal;
			System.out.println("kitab " + kitab.judul + " offset: " + kitab.offset);
			
			xkitab_[kitabPos] = kitab;
			
			//# reset
			offsetTotal += offsetLewat;
			offsetLewat = 0;
			for (int i = 0; i < xnayat.length; i++) xnayat[i] = 0;
			maxpasal_1 = 1;
			lastpasal_1 = 0;
		}
		return new InfoKitab() {{
			this.xkitab = xkitab_;
		}};
	}
}
