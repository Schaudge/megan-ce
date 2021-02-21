/*
 * CompareByAttributeCommand.java Copyright (C) 2020. Daniel H. Huson
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
package megan.commands.additional;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.swing.util.ResourceManager;
import jloda.swing.window.NotificationsInSwing;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.commands.NewCommand;
import megan.commands.algorithms.ComputeCoreBiome;
import megan.core.*;
import megan.dialogs.compare.Comparer;
import megan.main.MeganProperties;
import megan.viewer.gui.NodeDrawer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * * split by command
 * * Daniel Huson, 11.2020
 */
public class SplitByAttributeCommand extends CommandBase implements ICommand {
    public String getSyntax() {
        return "splitBy attribute=<name> [samples=<names>];";
    }

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    public void apply(NexusStreamParser np) throws Exception {
        final Document doc = ((Director) getDir()).getDocument();

        np.matchIgnoreCase("splitBy attribute=");
        String attribute = np.getWordRespectCase();

        final ArrayList<String> srcSamples = new ArrayList<>();
        if (np.peekMatchIgnoreCase("samples")) {
            srcSamples.addAll(np.getTokensRespectCase("samples=", ";"));
        } else {
            srcSamples.addAll(doc.getSampleNames());
            np.matchIgnoreCase(";");
        }

        final Map<String, List<String>> tarSample2SrcSamples = new HashMap<>();

        for (String sample : srcSamples) {
            final Object obj = doc.getSampleAttributeTable().get(sample, attribute);
            if (obj != null) {
                final String value =  Basic.toCleanName(obj.toString());
                if (value.length() > 0) {
                    final String tarSample =attribute.equals(SampleAttributeTable.SAMPLE_ID) ? value : attribute + "-" + value;
                    tarSample2SrcSamples.computeIfAbsent(tarSample, k -> new ArrayList<>());
                    tarSample2SrcSamples.get(tarSample).add(sample);
                }
            }
        }

        final List<String> commands=new ArrayList<>();

        if (tarSample2SrcSamples.size() > 0) {
            for(String tarName:tarSample2SrcSamples.keySet()) {
                final String fileName = Basic.replaceFileSuffix(Basic.getFileNameWithoutPath(doc.getMeganFile().getFileName()), "-"+tarName+ ".megan");
                  final List<String> samples=tarSample2SrcSamples.get(tarName);
                  if(samples.size()>0)
                    commands.add(String.format("extract samples='%s' file='%s';",Basic.toString(samples,"' '"),fileName));
            }
        }
        executeImmediately(Basic.toString(commands,"\n"));
    }

    public void actionPerformed(ActionEvent event) {
    }

    public boolean isApplicable() {
        return true;
    }

    public String getName() {
        return null;
    }

    public String getDescription() {
        return "Splits samples by this attribute and show in new documents";
    }

    public ImageIcon getIcon() {
        return ResourceManager.getIcon("Compare16.gif");
    }

    public boolean isCritical() {
        return true;
    }

    public KeyStroke getAcceleratorKey() {
        return null;
    }

}