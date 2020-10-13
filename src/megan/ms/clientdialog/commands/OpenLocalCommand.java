/*
 * OpenRemoteServerCommand.java Copyright (C) 2020. Daniel H. Huson
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
package megan.ms.clientdialog.commands;

import jloda.swing.commands.CommandBase;
import jloda.swing.commands.ICommand;
import jloda.util.Basic;
import jloda.util.ProgramProperties;
import jloda.util.parse.NexusStreamParser;
import megan.ms.clientdialog.RemoteServiceBrowser;
import megan.ms.clientdialog.service.RemoteServiceManager;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;

/**
 * command
 * Daniel Huson, 12.2014
 */
public class OpenLocalCommand extends CommandBase implements ICommand {

    /**
     * parses the given command and executes it
     *
     * @param np
     * @throws java.io.IOException
     */
    @Override
    public void apply(NexusStreamParser np) throws Exception {

    }

    /**
     * get command-line usage description
     *
     * @return usage
     */
    @Override
    public String getSyntax() {
        return null;
    }

    /**
     * action to be performed
     *
     * @param ev
     */
    @Override
    public void actionPerformed(ActionEvent ev) {
        final RemoteServiceBrowser remoteServiceBrowser = (RemoteServiceBrowser) getViewer();
        JFileChooser chooser = new JFileChooser();
        chooser.setToolTipText("Select directory to browse");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        final File prev = new File(ProgramProperties.get("LocalServiceDirectory", System.getProperty("user.home")));
        if (Basic.isDirectory(prev.getParent()))
            chooser.setCurrentDirectory(prev.getParentFile());
        chooser.setSelectedFile(prev);

        int result = chooser.showOpenDialog(remoteServiceBrowser);
        if (result == JFileChooser.APPROVE_OPTION) {
            final File dir = chooser.getSelectedFile();
            if (dir != null) {
                remoteServiceBrowser.setURL(RemoteServiceManager.LOCAL + dir.getPath());
                remoteServiceBrowser.clearUser();
                remoteServiceBrowser.clearPassword();
                execute("openServer url=" + remoteServiceBrowser.getURL() + ";");
                ProgramProperties.put("LocalServiceDirectory", dir.getPath());
            }
        }


    }

    public static final String NAME = "Open Local Directory...";

    public String getName() {
        return NAME;
    }

    /**
     * get description to be used as a tooltip
     *
     * @return description
     */
    public String getDescription() {
        return "Open a local directory";
    }

    /**
     * get icon to be used in menu or button
     *
     * @return icon
     */
    public ImageIcon getIcon() {
        return null;
    }

    /**
     * gets the accelerator key  to be used in menu
     *
     * @return accelerator key
     */
    public KeyStroke getAcceleratorKey() {
        return null;
    }

    /**
     * is this a critical command that can only be executed when no other command is running?
     *
     * @return true, if critical
     */
    public boolean isCritical() {
        return true;
    }

    /**
     * is the command currently applicable? Used to set enable state of command
     *
     * @return true, if command can be applied
     */
    public boolean isApplicable() {
        if (getViewer() == null || !(getViewer() instanceof RemoteServiceBrowser))
            return false;
        final RemoteServiceBrowser remoteServiceBrowser = (RemoteServiceBrowser) getViewer();

        return !remoteServiceBrowser.isServiceSelected() && remoteServiceBrowser.getURL().length() > 0;
    }
}
