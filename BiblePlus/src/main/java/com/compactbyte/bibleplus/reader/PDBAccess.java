/*
    Bible Plus A Bible Reader for Blackberry
    Copyright (C) 2010 Yohanes Nugroho

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

    Yohanes Nugroho (yohanes@gmail.com)
 */

package com.compactbyte.bibleplus.reader;

import java.io.*;

/**
 * Class to access any Palm PDB file.
 * 
 */
class PDBAccess {

	private boolean read_all;

	private String filename;
	private int filesize;
	private PDBHeader header;
	private PDBRecord[] records;
	private byte[] header_data;
	private PDBDataStream is;

	private int record_offsets[];
	private int record_attrs[];

	private boolean is_corrupted;

	public PDBAccess(PDBDataStream is) {
		this.is = is;
	}

	void close() {
		try {
			is.close();
			// make sure it is garbage collected
			header = null;
			if (records != null) for (int i = 0; i < records.length; i++) {
				records[i] = null;
			}
			records = null;
			header_data = null;
			is = null;
			record_offsets = null;
			record_attrs = null;

		} catch (IOException e) {

		}

	}

	/**
	 * Read and parse PDB header
	 * 
	 * @return PDBHeader object
	 */
	public PDBHeader getHeader() throws IOException {

		if (header == null) {
			header_data = new byte[78];
			is.read(header_data);
			header = new PDBHeader(header_data);
			header.load();
			// System.out.println(header);
			records = new PDBRecord[header.getRecordCount()];
			readOffsets();

			if (record_offsets[record_offsets.length - 1] > is.getSize()) {
				is_corrupted = true;
				return null;
			}

			if (!is.canSeek()) {
				readAll();
			}
		}
		return header;
	}

	/**
	 * Check if the file is corrupted (invalid offset table)
	 */
	public boolean isCorrupted() {
		return is_corrupted;
	}

	private void loadRecord(int recno) throws IOException {
		int length = 0;
		if (recno < records.length - 1) {
			length = record_offsets[recno + 1] - record_offsets[recno];
		} else {
			length = (int) is.getSize() - record_offsets[recno];
		}
		is.seek(record_offsets[recno]);
		byte[] data = new byte[length];
		is.read(data);
		PDBRecord pr = new PDBRecord(data);
		records[recno] = pr;
	}

	/**
	 * read all record to memory if we can not seek
	 */
	private void readAll() throws IOException {
		// System.out.println("reading all");
		for (int i = 0; i < header.getRecordCount(); i++) {
			loadRecord(i);
		}
	}

	private void readOffsets() throws IOException {

		int n = header.getRecordCount();

		byte[] temp_read = new byte[8 * n];
		is.read(temp_read);
		int offs = 0;
		record_offsets = new int[n];
		record_attrs = new int[n];
		for (int i = 0; i < n; i++) {

			int val = (((temp_read[offs + 0] & 0xff) << 24)
					| ((temp_read[offs + 1] & 0xff) << 16)
					| ((temp_read[offs + 2] & 0xff) << 8)
					| (temp_read[offs + 3] & 0xff));

			record_offsets[i] = val;
			// System.out.println("offsets: " + record_offsets[i]);
			record_attrs[i] = temp_read[offs + 4];
			offs += 8;
		}
	}

	/**
	 * Read a record
	 * 
	 * @param recno
	 *            record number
	 * @return PDBRecord object
	 */
	public PDBRecord readRecord(int recno) throws IOException {
		PDBRecord pr = records[recno];
		if (pr == null) {
			if (is.canSeek()) {
				loadRecord(recno);
			}
		}
		return records[recno];
	}

	/**
	 * remove record from cache
	 */
	public void removeFromCache(int recno) {
		records[recno] = null;
	}

}
