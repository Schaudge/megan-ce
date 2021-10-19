/*
 * AlignMode.java Copyright (C) 2021. Daniel H. Huson
 *
 *  (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package megan.daa.io;

import jloda.seq.BlastMode;

/**
 * alignment mode enum
 * Daniel Huson, 8.2015
 */
public enum AlignMode {
	blastp, blastx, blastn;

	public static byte rank(AlignMode mode) {
		switch (mode) {
            case blastp:
                return 2;
            case blastx:
                return 3;
            case blastn:
                return 4;
            default:
                return -1;
        }
    }

    public static AlignMode value(int rank) {
        switch (rank) {
            case 2:
                return blastp;
            case 3:
                return blastx;
            default:
            case 4:
                return blastn;
        }
    }

	public static BlastMode getBlastMode(int rank) {
		switch (rank) {
			case 2:
				return BlastMode.BlastP;
			case 3:
				return BlastMode.BlastX;
			default:
			case 4:
				return BlastMode.BlastN;
		}

    }
}
