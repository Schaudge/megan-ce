/*
 * MatchesExporter.java Copyright (C) 2023 Daniel H. Huson
 *
 * (Some files contain contributions from other authors, who are then mentioned separately.)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package megan.dialogs.export;

import jloda.seq.BlastMode;
import jloda.util.CanceledException;
import jloda.util.FileUtils;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import megan.classification.ClassificationManager;
import megan.daa.connector.MatchBlockDAA;
import megan.data.IConnector;
import megan.data.IMatchBlock;
import megan.data.IReadBlock;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;

/**
 * export all  matches to a file (or those associated with the set of selected taxa, if any selected)
 * Daniel Huson, 6.2010
 */
public class MatchesExporter {
	/**
	 * export all matches in file
	 */
	public static long exportAll(BlastMode blastMode, IConnector connector, String fileName, ProgressListener progressListener) throws IOException {
		progressListener.setTasks("Export", "Writing all matches");

		long total = 0;
		try {
			var asTab = fileName.endsWith(".tab") || fileName.endsWith(".tab.gz") || fileName.endsWith(".blasttab") || fileName.endsWith(".blasttab.gz");

			try (var w = new BufferedWriter(FileUtils.getOutputWriterPossiblyZIPorGZIP(fileName));
				 var it = connector.getAllReadsIterator(0, 10000, true, true)) {
				if (!asTab)
					w.write(blastMode.toString().toUpperCase() + " file generated by MEGAN6\n\n");
				progressListener.setMaximum(it.getMaximumProgress());
				progressListener.setProgress(0);
				while (it.hasNext()) {
					if (asTab)
						total += writeMatchesBlastTab(it.next(), w);
					else
						total += writeMatches(it.next(), w);
					progressListener.setProgress(it.getProgress());
				}
			}
		} catch (CanceledException ex) {
			System.err.println("USER CANCELED");
		}
		return total;
	}

	/**
	 * export all matches for given set of classids in the given classification
	 */
	public static long export(String classificationName, Collection<Integer> classIds, BlastMode blastMode, IConnector connector, String fileName, ProgressListener progressListener) throws IOException {
		long total = 0;
		BufferedWriter w = null;

			try {
			progressListener.setTasks("Export", "Writing selected matches");

            var asTab = fileName.endsWith(".tab") || fileName.endsWith(".tab.gz") || fileName.endsWith(".blasttab") || fileName.endsWith(".blasttab.gz");

				if(fileName.contains("%f")) {
					fileName = fileName.replaceAll("%f", FileUtils.getFileNameWithoutPathOrSuffix(connector.getFilename()));
				}
				final var useOneOutputFile = (!fileName.contains("%t") && !fileName.contains("%i"));
			final var classification = (!useOneOutputFile?ClassificationManager.get(classificationName, true):null);

			var maxProgress = 100000 * classIds.size();
			var currentProgress = 0;
			progressListener.setMaximum(maxProgress);
			progressListener.setProgress(currentProgress);
			var count = 0;

			for (var classId : classIds) {
				if (useOneOutputFile) {
					if (w == null)
						w = new BufferedWriter(FileUtils.getOutputWriterPossiblyZIPorGZIP(fileName));
				} else {
					if (w != null)
						w.close();
					var cName = classification.getName2IdMap().get(classId);
					var fName = fileName.replaceAll("%t", StringUtils.toCleanName(cName)).replaceAll("%i", "" + classId);
					w = new BufferedWriter(FileUtils.getOutputWriterPossiblyZIPorGZIP(fName));
				}

				count++;
				currentProgress = 100000 * count;
				try (var it = connector.getReadsIterator(classificationName, classId, 0, 10000, true, true)) {
					var progressIncrement = 100000 / (it.getMaximumProgress() + 1);
					while (it.hasNext()) {
						if (asTab)
							total += writeMatchesBlastTab(it.next(), w);
						else
							total += writeMatches(it.next(), w);
						progressListener.setProgress(currentProgress);
						currentProgress += progressIncrement;
					}
				}
			}

		} catch (CanceledException ex) {
			System.err.println("USER CANCELED");
		} finally {
			if (w != null)
				w.close();
		}
		return total;
	}

	/**
	 * write all matches associated with the given read
	 *
	 * @return number of matches written
	 */
	private static int writeMatches(IReadBlock readBlock, Writer w) throws IOException {
		int countMatches = 0;
		String readHeader = readBlock.getReadHeader();
		if (readHeader.startsWith(">"))
			readHeader = readHeader.substring(1);
		w.write("\nQuery=" + readHeader + "\n");

		final String readSequence = readBlock.getReadSequence();
		if (readSequence != null)
			w.write("\t(" + readSequence.length() + " letters)\n");
		w.write("\n");

        /*
        FastA fastA = new FastA(readBlock.getValueString(getReadFormat().getHeadItem()),
                readBlock.getValueString(getReadFormat().getSequenceItem()));
        fastA.write(w);
        */

		if (readBlock.getNumberOfAvailableMatchBlocks() == 0)
			w.write(" ***** No hits found ******\n");
		else {
			for (IMatchBlock matchBlock : readBlock.getMatchBlocks()) {
				w.write(matchBlock.getText() + "\n");
				countMatches++;
			}
		}
		return countMatches;
	}

	/**
	 * write all matches associated with the given read
	 *
	 * @return number of matches written
	 */
	private static int writeMatchesBlastTab(IReadBlock readBlock, Writer w) throws IOException {
		var countMatches = 0;
		if (readBlock.getNumberOfAvailableMatchBlocks() == 0)
			w.write(readBlock.getReadName() + "\n");
		else {
			for (IMatchBlock matchBlock : readBlock.getMatchBlocks()) {
				if (matchBlock instanceof MatchBlockDAA matchBlockDAA) {
					w.write(matchBlockDAA.getTextBlastTab());
					countMatches++;
				}
			}
		}
		return countMatches;
	}
}
