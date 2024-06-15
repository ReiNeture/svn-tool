package fubuki.ref;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.Set;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

public class DiffGenerator {

    private final SVNClientManager clientManager;
    private final SVNDiffClient diffClient;

    public DiffGenerator(SVNClientManager clientManager) {
        this.clientManager = clientManager;
        this.diffClient = clientManager.getDiffClient();
    }

    public void generateDiffs(SVNURL url, Set<String> modifiedFiles, long startRevision, long endRevision, String outputDir, boolean preserveFileStructure) throws SVNException {
        Map<String, Integer> fileNameCount = SVNUtilities.getFileNameCount(modifiedFiles);

        for (String file : modifiedFiles) {
            if (!SVNUtilities.fileExistsInRevision(url, file, endRevision, clientManager)) {
                System.out.println("Skipping non-existent file: " + file);
                continue;
            }

            String diffFileName = preserveFileStructure ? outputDir + File.separator + file + ".diff" : getDiffFileName(file, fileNameCount, outputDir);
            File diffFile = new File(diffFileName);
            createParentDirs(diffFile);

            try (OutputStream writer = new FileOutputStream(diffFile)) {
                diffClient.doDiff(url.appendPath(file, false), SVNRevision.create(startRevision),
                        url.appendPath(file, false), SVNRevision.create(endRevision),
                        SVNDepth.INFINITY, true, writer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            // 檢查是否為空文件
            if (diffFile.length() == 0) {
                System.out.println("Deleting empty diff file: " + diffFile.getPath());
                diffFile.delete();
            }
        }
    }

    private String getDiffFileName(String filePath, Map<String, Integer> fileNameCount, String outputDir) {
        String fileName = new File(filePath).getName();
        if (fileNameCount.get(fileName) > 1) {
            String parentPath = SVNUtilities.getParentDirectoryName(filePath);
            return outputDir + File.separator + parentPath + "." + fileName + ".diff";
        } else {
            return outputDir + File.separator + fileName + ".diff";
        }
    }

    private void createParentDirs(File file) {
        File parentDir = file.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
    }
    
    public static boolean isDiffEmpty(SVNURL url, String filePath, long startRevision, long endRevision, SVNClientManager clientManager) throws SVNException {
        SVNDiffClient diffClient = clientManager.getDiffClient();
        try (ByteArrayOutputStream diffStream = new ByteArrayOutputStream()) {
            diffClient.doDiff(url.appendPath(filePath, false), SVNRevision.create(startRevision), 
                              url.appendPath(filePath, false), SVNRevision.create(endRevision), 
                              SVNDepth.INFINITY, true, diffStream);
            return diffStream.size() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
