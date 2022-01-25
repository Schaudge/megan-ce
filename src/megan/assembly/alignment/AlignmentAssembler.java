/*
 * AlignmentAssembler.java Copyright (C) 2022 Daniel H. Huson
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
package megan.assembly.alignment;

import jloda.graph.*;
import jloda.graph.io.GraphGML;
import jloda.util.CanceledException;
import jloda.util.Pair;
import jloda.util.StringUtils;
import jloda.util.progress.ProgressListener;
import jloda.util.progress.ProgressPercentage;
import megan.alignment.gui.Alignment;
import megan.assembly.OverlapGraphViewer;
import megan.assembly.PathExtractor;
import megan.core.Director;

import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * assembles from an alignment
 * Daniel Huson, 5.2015
 */
public class AlignmentAssembler {
    private Graph overlapGraph;
    private Alignment alignment;
    private Node[][] paths;
    private NodeArray<String> node2readName;
    private ArrayList<Pair<String, String>> contigs;
    private List<Integer>[] readId2ContainedReads;

    /**
     * constructor
     */
    public AlignmentAssembler() {
    }

    /**
     * compute the overlap graph
     *
     * @param minOverlap
     * @param alignment
     * @param progress
     * @throws IOException
     * @throws CanceledException
     */
    public void computeOverlapGraph(int minOverlap, final Alignment alignment, ProgressListener progress) throws IOException {
        this.alignment = alignment;
        var overlapGraphBuilder = new OverlapGraphBuilder(minOverlap);
        overlapGraphBuilder.apply(alignment, progress);
        overlapGraph = overlapGraphBuilder.getOverlapGraph();
        node2readName = overlapGraphBuilder.getNode2ReadNameMap();
        readId2ContainedReads = overlapGraphBuilder.getReadId2ContainedReads();
    }

    /**
     * show the overlap graph
     *
     * @param dir
     * @param progress
     * @throws CanceledException
     */
    public void showOverlapGraph(Director dir, ProgressListener progress) throws CanceledException {
        final var overlapGraphViewer = new OverlapGraphViewer(dir, overlapGraph, node2readName, paths);
        overlapGraphViewer.apply(progress);
    }

    /**
     * write the overlap graph
     *
     * @param writer
     * @return
     * @throws IOException
     * @throws CanceledException
     */
    public Pair<Integer, Integer> writeOverlapGraph(Writer writer) throws IOException {
        try(NodeArray<String> nodeNameMap = new NodeArray<>(overlapGraph);
        NodeArray<String> nodeSequenceMap = new NodeArray<>(overlapGraph);
            EdgeArray<String> overlap = new EdgeArray<>(overlapGraph)) {

            for(var v:overlapGraph.nodes()) {
                var i = (Integer) v.getInfo();
                nodeSequenceMap.put(v, alignment.getLane(i).getBlock());
                nodeNameMap.put(v, StringUtils.getFirstWord(alignment.getLane(i).getName()));
            }
            final var label2nodes = new TreeMap<String, NodeArray<String>>();
            label2nodes.put("label", nodeNameMap);
            label2nodes.put("sequence", nodeSequenceMap);

            for(var e:overlapGraph.edges()) {
                overlap.put(e, e.getInfo().toString());
            }
            final var label2edges = new TreeMap<String, EdgeArray<String>>();
            label2edges.put("label", null);
            label2edges.put("overlap", overlap);

            GraphGML.writeGML(overlapGraph,"Overlap graph generated by MEGAN6",alignment.getName(),true,1,writer, label2nodes, label2edges);
            return new Pair<>(overlapGraph.getNumberOfNodes(), overlapGraph.getNumberOfEdges());
        }
    }

    /**
     * compute contigs. Also sorts alignment by contigs
     *
     * @param alignmentNumber
     * @param minReads
     * @param minCoverage
     * @param minLength
     * @param progress
     * @return
     * @throws CanceledException
     */
    public int computeContigs(int alignmentNumber, int minReads, double minCoverage, int minLength, boolean sortAlignmentByContigs, ProgressListener progress) throws CanceledException {
        final var pathExtractor = new PathExtractor(overlapGraph, readId2ContainedReads);
        pathExtractor.apply(progress);
        paths = pathExtractor.getPaths();

        final var contigBuilder = new ContigBuilder(pathExtractor.getPaths(), pathExtractor.getSingletons(), readId2ContainedReads);

        contigBuilder.apply(alignmentNumber, alignment, minReads, minCoverage, minLength, sortAlignmentByContigs, progress);
        contigs = contigBuilder.getContigs();

        return contigBuilder.getCountContigs();
    }

    public ArrayList<Pair<String, String>> getContigs() {
        return contigs;
    }

    /**
     * write contigs
     *
     * @param w
     * @param progress
     * @throws CanceledException
     * @throws IOException
     */
    public void writeContigs(Writer w, ProgressListener progress) throws IOException {
        progress.setSubtask("Writing contigs");
        progress.setMaximum(contigs.size());
        progress.setProgress(0);
        for (var pair : contigs) {
            w.write(pair.getFirst().trim());
            w.write("\n");
            w.write(pair.getSecond().trim());
            w.write("\n");
            progress.incrementProgress();
        }
        w.flush();
        progress.reportTaskCompleted();
    }
}
