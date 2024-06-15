package fubuki.ref;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNNodeKind;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNLogClient;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCClient;

public class SVNUtilities {

    public static Set<String> getModifiedFiles(SVNURL url, long startRevision, long endRevision, SVNClientManager clientManager) throws SVNException {
        Set<String> modifiedFiles = new HashSet<>();
        SVNLogClient logClient = clientManager.getLogClient();
        logClient.doLog(url, new String[]{""}, SVNRevision.UNDEFINED, SVNRevision.create(startRevision + 1),
                SVNRevision.create(endRevision), true, true, 0,
                logEntry -> modifiedFiles.addAll(logEntry.getChangedPaths().values().stream()
                        .filter(entryPath -> entryPath.getKind() == SVNNodeKind.FILE)
                        .map(SVNLogEntryPath::getPath)
                        .collect(Collectors.toSet())));

        return modifiedFiles;
    }

    public static boolean fileExistsInRevision(SVNURL url, String filePath, long revision, SVNClientManager clientManager) {
        SVNWCClient wcClient = clientManager.getWCClient();
        try {
            SVNURL fileUrl = url.appendPath(filePath, false);
            wcClient.doInfo(fileUrl, SVNRevision.create(revision), SVNRevision.create(revision));
            return true;
        } catch (SVNException e) {
            return false;
        }
    }
    
    public static String getParentDirectoryName(String filePath) {
        Path path = Paths.get(filePath);
        Path parent = path.getParent();
        if (parent != null) {
            return parent.getFileName().toString();
        }
        return null;
    }

    public static Map<String, Integer> getFileNameCount(Set<String> modifiedFiles) {
        Map<String, Integer> fileNameCount = new HashMap<>();
        for (String file : modifiedFiles) {
            String fileName = new File(file).getName();
            fileNameCount.put(fileName, fileNameCount.getOrDefault(fileName, 0) + 1);
        }
        return fileNameCount;
    }
}
