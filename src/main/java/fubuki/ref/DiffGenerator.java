package fubuki.ref;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNDiffClient;
import org.tmatesoft.svn.core.wc.SVNRevision;

import fubuki.ref.entry.ModifiedFileEntry;

public class DiffGenerator {

    private final SVNClientManager clientManager;
    private final SVNDiffClient diffClient;

    public DiffGenerator(SVNClientManager clientManager) {
        this.clientManager = clientManager;
        this.diffClient = clientManager.getDiffClient();
    }

    public void generateDiffs(SVNURL url, List<ModifiedFileEntry> modifiedFiles, long startRevision, long endRevision, String outputDir, boolean preserveFileStructure) {
        Map<String, Integer> fileNameCount = SVNUtilities.getFileNameCount(modifiedFiles);

        SVNUtilities.cleanDirectory(outputDir);
        
        Iterator<ModifiedFileEntry> iterator = modifiedFiles.iterator();
        
        while (iterator.hasNext()) {
            ModifiedFileEntry fileEntry = iterator.next();
            String file = fileEntry.getEntryPath().getPath();
        	
        	if (SVNUtilities.isBinaryFile(file)) {
                System.out.println("跳過非文字檔: " + file);
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
            	System.err.println(e.getMessage());
            }
            
	        if (diffFile.length() == 0) {
	            System.err.println("已從清單中排除: " + diffFile.getPath());
	            diffFile.delete();
	            iterator.remove();  // 從 modifiedFiles 集合中移除無異動的檔案
	            
	            if(preserveFileStructure) {
	                File parentDir = diffFile.getParentFile();
	                deleteEmptyDirs(parentDir, new File(outputDir));
	            }
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
    
    public static boolean isDiffEmpty(SVNURL url, String filePath, long startRevision, long endRevision, SVNClientManager clientManager) {
        SVNDiffClient diffClient = clientManager.getDiffClient();
        try (ByteArrayOutputStream diffStream = new ByteArrayOutputStream()) {
            diffClient.doDiff(url.appendPath(filePath, false), SVNRevision.create(startRevision), 
                              url.appendPath(filePath, false), SVNRevision.create(endRevision), 
                              SVNDepth.INFINITY, true, diffStream);
            return diffStream.size() == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return true;
        }
    }
    
	private static void deleteEmptyDirs(File dir, File outputDir) {
        if (dir.isDirectory() && dir.list().length == 0 
        		&& !dir.equals(outputDir) ) {
        	
            System.out.println("移除空資料夾: " + dir.getPath());
            dir.delete();
            deleteEmptyDirs(dir.getParentFile(), outputDir);
        }
    }
}
