package fubuki.ref;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
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

import fubuki.ref.entry.ModifiedFileEntry;

public class SVNUtilities {

	private static final Set<String> BINARY_EXTENSIONS = new HashSet<>(Arrays.asList(
	        "gif", "jpg", "jpeg", "png", "bmp", "tiff", "ico", "mp3", "wav", "ogg",
	        "avi", "mp4", "mov", "mkv", "wmv", "flv", "pdf", "doc", "docx", "xls",
	        "xlsx", "ppt", "pptx", "exe", "dll", "bin", "class", "jar", "rpt"
	    ));
	
    public static Set<String> getModifiedPaths(SVNURL url, long startRevision, long endRevision, SVNClientManager clientManager) throws SVNException {
        Set<String> modifiedFiles = new HashSet<>();
        SVNLogClient logClient = clientManager.getLogClient();
        logClient.doLog(url, new String[]{"/TBBWeb/src/control/ExecuteflowServlet.java"}, SVNRevision.UNDEFINED, SVNRevision.create(startRevision + 1),
                SVNRevision.create(endRevision), true, true, 0,
                logEntry -> modifiedFiles.addAll(logEntry.getChangedPaths().values().stream()
                        .filter(entryPath -> entryPath.getKind() == SVNNodeKind.FILE)
                        .map(SVNLogEntryPath::getPath)
                        .collect(Collectors.toSet())));

        return modifiedFiles;
    }

    public static Set<ModifiedFileEntry> getModifiedFiles(SVNURL url, long startRevision, long endRevision, SVNClientManager clientManager) throws SVNException {
        Set<ModifiedFileEntry> modifiedFiles = new HashSet<>();
        SVNLogClient logClient = clientManager.getLogClient();
        logClient.doLog(url, new String[]{"/TBBWeb/src/control/ExecuteflowServlet.java"}, SVNRevision.UNDEFINED, SVNRevision.create(startRevision + 1),
                SVNRevision.create(endRevision), true, true, 0,
                logEntry -> modifiedFiles.addAll(
                        logEntry.getChangedPaths().values().stream()
                                .filter(entryPath -> entryPath.getKind() == SVNNodeKind.FILE)
                                .map(entryPath -> new ModifiedFileEntry(entryPath, logEntry.getDate()))
                                .collect(Collectors.toList())));

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
    
    public static void cleanDirectory(String directoryPath) {
        Path directory = Paths.get(directoryPath);
        try {
        	
        	if (!Files.exists(directory)) {
                Files.createDirectories(directory);
                return;
            }
        	
            Files.walk(directory)
                 .sorted(java.util.Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
            Files.createDirectories(directory);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static String getFileExtension(String filePath) {
        String fileName = new File(filePath).getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > -1 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1);
        }
        return "";
    }
    
    public static boolean isBinaryFile(String filePath) {
        String extension = getFileExtension(filePath).toLowerCase();
        return BINARY_EXTENSIONS.contains(extension);
    }
    
}
