/*
 * RemoteService.java Copyright (C) 2020. Daniel H. Huson
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
package megan.ms.clientdialog.service;

import jloda.util.Basic;
import megan.ms.client.ClientMS;
import megan.ms.clientdialog.IRemoteService;
import megan.ms.server.MeganServer;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * implements a remote service
 * <p/>
 * Created by huson on 10/3/14.
 */
public class RemoteService implements IRemoteService {
    private final String serverURL; // server URL, e.g. http://localhost:8080 or http://localhost:8080/Megan5Server
    private final ClientMS clientMS;
    private final List<String> files;

    private String about;

    private final Map<String, String> fileName2Description = new HashMap<>();

    /**
     * constructor
     *
     * @path path to root directory
     */
    public RemoteService(String serverURL, String user, String passwordHash) throws IOException {
        serverURL = serverURL.replaceAll("/$", "");
        if (!serverURL.contains(("/")))
            serverURL += "/megan6server";
        this.serverURL = serverURL;

        clientMS = new ClientMS(this.serverURL, null, 0, user, passwordHash, 100);

        final String remoteVersion = clientMS.getAsString("version");
        if (!remoteVersion.startsWith("MeganServer"))
            throw new IOException("Failed to confirm MeganServer at remote site");
        if (!remoteVersion.equals(MeganServer.Version))
            throw new IOException("Incompatible version numbers: client=" + MeganServer.Version + " server=" + remoteVersion);

        about = clientMS.getAsString("about");

        System.err.println(about);

        files = clientMS.getFiles();
        for (String file : files) {
            final String description;
            int reads = clientMS.getAsInt("getNumberOfReads?file=" + file);
            int matches = clientMS.getAsInt("getNumberOfMatches?file=" + file);
            if (reads > 0 && matches > 0)
                description = String.format("Reads: %,d, matches: %,d", reads, matches);
            else if (reads > 0)
                description = String.format("Reads: %,d", reads);
            else
                description = Basic.getFileNameWithoutPath(file);
            fileName2Description.put(file, description);
        }
        System.err.println("Server: " + serverURL + ", number of available files: " + getAvailableFiles().size());
    }


    /**
     * is this node available?
     *
     * @return availability
     */
    @Override
    public boolean isAvailable() {
        return true; // todo: fix
    }

    /**
     * get a list of available files and their unique ids
     *
     * @return list of available files in format path,id
     */
    @Override
    public List<String> getAvailableFiles() {
        return files;
    }

    /**
     * get the server URL
     *
     * @return server URL
     */
    @Override
    public String getServerURL() {
        return serverURL;
    }

    /**
     * gets the server and file name
     *
     * @param file
     * @return server and file
     */
    @Override
    public String getServerAndFileName(String file) {
        return serverURL + "::" + file;
    }

    /**
     * gets the info string for a server
     *
     * @return info in html
     */
    @Override
    public String getInfo() {
        try {
            return clientMS.getAsString("about");
        } catch (IOException ignored) {
        }
        return "";
    }

    /**
     * get the description associated with a given file name
     *
     * @param fileName
     * @return description
     */
    public String getDescription(String fileName) {
        return fileName2Description.get(fileName);
    }

    public ClientMS getClientMS() {
        return clientMS;
    }

    public String getAbout() {
        return about;
    }
}


