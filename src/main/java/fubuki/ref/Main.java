package fubuki.ref;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.util.Set;

public class Main {

    public static void main(String[] args) {
        final String repoUrl = "http://192.168.18.207:8085/svn/TBB_edge";  // your svn repo URL
        long startRevision = 555;
        long endRevision = 560;
        final String outputDir = "./svn_diffs";
        final String exportDir = "./svn_source";
        boolean preserveFileStructure = false; // diff need to create directory structure for files?

        try {
            SVNURL url = SVNURL.parseURIEncoded(repoUrl);
            SVNClientManager clientManager = SVNClientManager.newInstance();

            Set<String> modifiedFiles = SVNUtilities.getModifiedFiles(url, startRevision, endRevision, clientManager);

            DiffGenerator diffGenerator = new DiffGenerator(clientManager);
            diffGenerator.generateDiffs(url, modifiedFiles, startRevision, endRevision, outputDir, preserveFileStructure);
            System.out.println("Diff generate completed.");
            
            FileExporter fileExporter = new FileExporter(clientManager);
            fileExporter.exportFiles(url, modifiedFiles, startRevision, endRevision, exportDir);
            System.out.println("Source Export completed.");
            
            
        } catch (SVNException e) {
            e.printStackTrace();
        }
    }
}
