package fubuki.ref;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
        logClient.doLog(url, new String[]{""}, SVNRevision.UNDEFINED, SVNRevision.create(startRevision + 1),
                SVNRevision.create(endRevision), true, true, 0,
                logEntry -> modifiedFiles.addAll(logEntry.getChangedPaths().values().stream()
                        .filter(entryPath -> entryPath.getKind() == SVNNodeKind.FILE)
                        .map(SVNLogEntryPath::getPath)
                        .collect(Collectors.toSet())));

        return modifiedFiles;
    }

    public static List<ModifiedFileEntry> getModifiedFiles(SVNURL url, String revisionRange, SVNClientManager clientManager, String branchPath) throws SVNException {
    	List<Long> revisions = parseRevisions(revisionRange);
    	Set<Long> revisionSet = new HashSet<>(revisions); // 使用 Set 以便快速查找
        Map<String, ModifiedFileEntry> modifiedFilesMap = new HashMap<>();
        SVNLogClient logClient = clientManager.getLogClient();
        
        // 設置一次性的版本區間查詢
        SVNRevision startRevision = SVNRevision.create(revisions.get(0));
        SVNRevision endRevision = SVNRevision.create(revisions.get(revisions.size() - 1));
        
        logClient.doLog(url, new String[]{branchPath}, SVNRevision.UNDEFINED, startRevision, endRevision, true, true, 0,
                logEntry -> {
                    long logEntryRevision = logEntry.getRevision();
                    if (revisionSet.contains(logEntryRevision)) {
                        logEntry.getChangedPaths().values().stream()
                            .filter(entryPath -> entryPath.getKind() == SVNNodeKind.FILE)
                            .forEach(entryPath -> {
                                ModifiedFileEntry entry = modifiedFilesMap.computeIfAbsent(entryPath.getPath(), 
                                    k -> new ModifiedFileEntry(entryPath, logEntry.getDate(), logEntry.getRevision()));
                                entry.addOperation(entryPath.getType(), logEntry.getRevision());
                            });
                    }
                });

        return new ArrayList<>(modifiedFilesMap.values());
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
    
    public static Map<String, Integer> getFileNameCount(List<ModifiedFileEntry> modifiedFiles) {
        Map<String, Integer> fileNameCount = new HashMap<>();
        for (ModifiedFileEntry enrty : modifiedFiles) {
        	String file = enrty.getEntryPath().getPath();
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
    
    public static List<Long> parseRevisions(String revisionRange) {
        Set<Long> revisions = new TreeSet<>();
        String[] ranges = revisionRange.split(";");
        
        long lastRevision = Long.MIN_VALUE;
        for (String range : ranges) {
            if (range.contains("-")) {
                String[] bounds = range.split("-");
                long start = Long.parseLong(bounds[0]);
                long end = Long.parseLong(bounds[1]);

                // 檢查是否符合從小到大的規則
                if (start > end)
                    throw new IllegalArgumentException("Invalid revision range: " + range);
                // 檢查輸入順序是否從小到大
                if (start < lastRevision)
                    throw new IllegalArgumentException("Revision range out of order: " + range);
                
                for (long i = start; i <= end; i++)
                    revisions.add(i);

                lastRevision = end;
                
            } else {
                long revision = Long.parseLong(range);

                // 檢查輸入順序是否從小到大
                if (revision < lastRevision)
                    throw new IllegalArgumentException("Revision range out of order: " + range);

                revisions.add(revision);
                lastRevision = revision;
            }
        }

        return new ArrayList<>(revisions);
    }
    
}
