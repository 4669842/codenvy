/*
 * CODENVY CONFIDENTIAL
 * __________________
 *
 *  [2012] - [2015] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.im.service;

import com.codenvy.im.artifacts.Artifact;
import com.codenvy.im.artifacts.InstallManagerArtifact;
import com.codenvy.im.backup.BackupConfig;
import com.codenvy.im.backup.BackupManager;
import com.codenvy.im.install.InstallOptions;
import com.codenvy.im.install.Installer;
import com.codenvy.im.node.NodeConfig;
import com.codenvy.im.node.NodeManager;
import com.codenvy.im.utils.HttpException;
import com.codenvy.im.utils.HttpTransport;
import com.codenvy.im.utils.Version;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.commons.json.JsonParseException;

import javax.inject.Named;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import static com.codenvy.im.artifacts.ArtifactProperties.FILE_NAME_PROPERTY;
import static com.codenvy.im.artifacts.ArtifactProperties.SIZE_PROPERTY;
import static com.codenvy.im.utils.ArtifactPropertiesUtils.isAuthenticationRequired;
import static com.codenvy.im.utils.Commons.ArtifactsSet;
import static com.codenvy.im.utils.Commons.combinePaths;
import static com.codenvy.im.utils.Commons.extractServerUrl;
import static com.codenvy.im.utils.Commons.getProperException;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.delete;
import static org.apache.commons.io.FileUtils.deleteDirectory;

/**
 * @author Anatoliy Bazko
 * @author Dmytro Nochevnov
 */
@Singleton
public class InstallationManagerImpl implements InstallationManager {
    private final String        updateEndpoint;
    private final Installer     installer;
    private final HttpTransport transport;
    private final Set<Artifact> artifacts;
    private final NodeManager   nodeManager;
    private final BackupManager backupManager;
    private final Path downloadDir;

    @Inject
    public InstallationManagerImpl(@Named("installation-manager.update_server_endpoint") String updateEndpoint,
                                   @Named("installation-manager.download_dir") String downloadDir,
                                   HttpTransport transport,
                                   Installer installer,
                                   Set<Artifact> artifacts,
                                   NodeManager nodeManager,
                                   BackupManager backupManager) throws IOException {
        this.updateEndpoint = updateEndpoint;
        this.transport = transport;
        this.installer = installer;
        this.artifacts = new ArtifactsSet(artifacts); // keep order

        this.downloadDir = Paths.get(downloadDir);
        checkRWPermissions(this.downloadDir);

        this.nodeManager = nodeManager;
        this.backupManager = backupManager;
    }

    private void checkRWPermissions(Path downloadDir) throws IOException {
        try {
            createDirectories(downloadDir);

            Path tmp = downloadDir.resolve("tmp.tmp");
            createFile(tmp);
            delete(tmp);
        } catch (IOException e) {
            throw new IOException("Installation Manager probably doesn't have r/w permissions to " + downloadDir.toString(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getInstallInfo(Artifact artifact, Version version, InstallOptions options) throws IOException {
        return installer.getInstallInfo(artifact, version, options);
    }

    /** {@inheritDoc} */
    @Override
    public void install(String authToken, Artifact artifact, Version version, InstallOptions options) throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloadedArtifacts = getDownloadedArtifacts();

        if (downloadedArtifacts.containsKey(artifact) && downloadedArtifacts.get(artifact).containsKey(version)) {
            Path pathToBinaries = downloadedArtifacts.get(artifact).get(version);
            if (pathToBinaries == null) {
                throw new IOException(String.format("Binaries for artifact '%s' version '%s' not found", artifact, version));
            }

            if (options.getStep() != 0 || artifact.isInstallable(version, updateEndpoint, transport)) {
                installer.install(artifact, version, pathToBinaries, options);
            } else {
                throw new IllegalStateException("Can not install the artifact '" + artifact.getName() + "' version '" + version + "'.");
            }
        } else {
            throw new FileNotFoundException("Binaries to install artifact '" + artifact.getName() + "' version '" + version + "' not found");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, Version> getInstalledArtifacts(String authToken) throws IOException {
        Map<Artifact, Version> installed = new LinkedHashMap<>();
        for (Artifact artifact : artifacts) {
            try {
                Version installedVersion = artifact.getInstalledVersion();
                if (installedVersion != null) {
                    installed.put(artifact, installedVersion);
                }
            } catch (IOException e) {
                throw getProperException(e, artifact);
            }
        }

        return installed;
    }

    /** {@inheritDoc} */
    @Override
    public Path download(UserCredentials userCredentials, Artifact artifact, Version version) throws IOException, IllegalStateException {
        try {
            boolean isAuthenticationRequired = isAuthenticationRequired(artifact.getName(), version.toString(), transport, updateEndpoint);

            String requestUrl;
            if (isAuthenticationRequired) {
                requestUrl = combinePaths(updateEndpoint,
                                          "/repository/download/" + artifact.getName() + "/" + version + "/" + userCredentials.getAccountId());
            } else {
                requestUrl = combinePaths(updateEndpoint,
                                          "/repository/public/download/" + artifact.getName() + "/" + version);
            }

            Path artifactDownloadDir = getDownloadDirectory(artifact, version);
            deleteDirectory(artifactDownloadDir.toFile());

            return transport.download(requestUrl, artifactDownloadDir, userCredentials.getToken());
        } catch (IOException e) {
            throw getProperException(e, artifact);
        } catch (JsonParseException e) {
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkEnoughDiskSpace(long requiredSize) throws IOException {
        long freeSpace = downloadDir.toFile().getFreeSpace();
        if (freeSpace < requiredSize) {
            throw new IOException(String.format("Not enough disk space. Required %d bytes but available only %d bytes", requiredSize, freeSpace));
        }
    }

    /** {@inheritDoc} */
    @Override
    public void checkIfConnectionIsAvailable() throws IOException {
        transport.doGet(combinePaths(updateEndpoint, "repository/properties/" + InstallManagerArtifact.NAME));
    }

    /** {@inheritDoc} */
    @Override
    public LinkedHashMap<String, String> getConfig() {
        return new LinkedHashMap<String, String>() {{
            put("download directory", downloadDir.toString());
            put("base url", extractServerUrl(updateEndpoint));
        }};
    }

    protected void validatePath(Path newDownloadDir) throws IOException {
        if (!newDownloadDir.isAbsolute()) {
            throw new IOException("Path must be absolute.");
        }
    }

    @Override
    public Path getPathToBinaries(Artifact artifact, Version version) throws IOException {
        Map properties = artifact.getProperties(version, updateEndpoint, transport);
        String fileName = properties.get(FILE_NAME_PROPERTY).toString();

        return getDownloadDirectory(artifact, version).resolve(fileName);
    }

    @Override
    public Long getBinariesSize(Artifact artifact, Version version) throws IOException, NumberFormatException {
        Map properties = artifact.getProperties(version, updateEndpoint, transport);
        String size = properties.get(SIZE_PROPERTY).toString();

        return Long.valueOf(size);
    }

    @Override
    public Map<Artifact, SortedMap<Version, Path>> getDownloadedArtifacts() throws IOException {
        Map<Artifact, SortedMap<Version, Path>> downloaded = new LinkedHashMap<>(artifacts.size());

        for (Artifact artifact : artifacts) {
            SortedMap<Version, Path> versions = artifact.getDownloadedVersions(downloadDir, updateEndpoint, transport);

            if (!versions.isEmpty()) {
                downloaded.put(artifact, versions);
            }
        }

        return downloaded;
    }

    @Override
    public SortedMap<Version, Path> getDownloadedVersions(Artifact artifact) throws IOException {
        return artifact.getDownloadedVersions(downloadDir, updateEndpoint, transport);
    }

    /** {@inheritDoc} */
    @Override
    public Map<Artifact, Version> getUpdates(String authToken) throws IOException {
        Map<Artifact, Version> newVersions = new LinkedHashMap<>();

        Version newVersion;
        for (Artifact artifact : artifacts) {
            try {
                newVersion = artifact.getLatestInstallableVersion(authToken, updateEndpoint, transport);
            } catch (HttpException e) {
                // ignore update server error when there is no version of certain artifact there
                if (e.getStatus() == javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode()) {
                    continue;
                }
                throw e;
            }

            if (newVersion != null) {
                newVersions.put(artifact, newVersion);
            }
        }

        return newVersions;
    }

    /** {@inheritDoc} */
    @Override
    public Version getLatestInstallableVersion(String authToken, Artifact artifact) throws IOException {
        return artifact.getLatestInstallableVersion(authToken, updateEndpoint, transport);
    }

    private Path getDownloadDirectory(Artifact artifact, Version version) {
        return downloadDir.resolve(artifact.getName()).resolve(version.toString());
    }

    /** Filters what need to download, either all updates or a specific one. */
    @Override
    public Map<Artifact, Version> getUpdatesToDownload(Artifact artifact, Version version, String authToken) throws IOException {
        if (artifact == null) {
            Map<Artifact, Version> latestVersions = getUpdates(authToken);
            Map<Artifact, Version> updates = new TreeMap<>(latestVersions);
            for (Map.Entry<Artifact, Version> e : latestVersions.entrySet()) {

                Artifact eachArtifact = e.getKey();
                Version eachVersion = e.getValue();
                if (eachArtifact.getDownloadedVersions(downloadDir, updateEndpoint, transport).containsKey(eachVersion)) {
                    updates.remove(eachArtifact);
                }
            }

            return updates;
        } else {
            if (version != null) {
                // verify if version had been already downloaded
                if (artifact.getDownloadedVersions(downloadDir, updateEndpoint, transport).containsKey(version)) {
                    return Collections.emptyMap();
                }

                return ImmutableMap.of(artifact, version);
            }

            final Version versionToUpdate = artifact.getLatestInstallableVersion(authToken, updateEndpoint, transport);
            if (versionToUpdate == null) {
                return Collections.emptyMap();
            }

            // verify if version had been already downloaded
            if (artifact.getDownloadedVersions(downloadDir, updateEndpoint, transport).containsKey(versionToUpdate)) {
                return Collections.emptyMap();
            }

            return ImmutableMap.of(artifact, versionToUpdate);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean isInstallable(Artifact artifact, Version version, String authToken) throws IOException {
        return artifact.isInstallable(version, updateEndpoint, transport);
    }

    /** {@inheritDoc}
     * @throws IllegalArgumentException if node's type isn't supported
     */
    @Override
    public NodeConfig addNode(String dns) throws IOException, IllegalArgumentException {
        return nodeManager.add(dns);
    }

    /**
     * {@inheritDoc}
     * @throws IllegalArgumentException if node's type isn't supported
     */
    @Override
    public NodeConfig removeNode(String dns) throws IOException, IllegalArgumentException {
        return nodeManager.remove(dns);
    }

    /** {@inheritDoc} */
    @Override
    public BackupConfig backup(BackupConfig config) throws IOException {
        return backupManager.backup(config);
    }

    /** {@inheritDoc} */
    @Override
    public void restore(BackupConfig config) throws IOException {
        backupManager.restore(config);
    }
}
