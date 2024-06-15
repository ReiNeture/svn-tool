package fubuki.ref;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import java.util.Set;

public class Main {

    public static void main(String[] args) {
        final String repoUrl = "";  // �������A�� SVN �x�s�w URL
        long startRevision = 576;
        long endRevision = 578;
        final String outputDir = "./svn_diffs";
        final String exportDir = "./svn_source";
        boolean preserveFileStructure = false; // DIFF�O�_�ƻs�ؿ����c

        try {
            SVNURL url = SVNURL.parseURIEncoded(repoUrl);
            SVNClientManager clientManager = SVNClientManager.newInstance();

            Set<String> modifiedFiles = SVNUtilities.getModifiedFiles(url, startRevision, endRevision, clientManager);

            DiffGenerator diffGenerator = new DiffGenerator(clientManager);
            diffGenerator.generateDiffs(url, modifiedFiles, startRevision, endRevision, outputDir, preserveFileStructure);

            FileExporter fileExporter = new FileExporter(clientManager);
            fileExporter.exportFiles(url, modifiedFiles, endRevision, exportDir);

            System.out.println("Operation completed.");

        } catch (SVNException e) {
            e.printStackTrace();
        }
    }
}
