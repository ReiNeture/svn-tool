package fubuki.ref;

import java.io.IOException;
import java.util.List;

import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import fubuki.ref.entry.ModifiedFileEntry;

public class Main {

    public static void main(String[] args) {
        final String repoUrl = "https://127.0.0.1/svn/yuuki";  // your svn repo URL
        final long startRevision = 572;
        final long endRevision = 578;
        final String outputDir = "./svn_diffs";
        final String exportDir = "./svn_source";
        final String reportPath = "./svn_report.xlsx";
        final boolean preserveFileStructure = false; // diff need to create directory structure for files?

        try {
            SVNURL url = SVNURL.parseURIEncoded(repoUrl);
            SVNClientManager clientManager = SVNClientManager.newInstance();

//            Set<String> modifiedPaths = SVNUtilities.getModifiedPaths(url, startRevision, endRevision, clientManager);
            List<ModifiedFileEntry> modifiedFiles = SVNUtilities.getModifiedFiles(url, startRevision, endRevision, clientManager);
            System.out.println("已取得版本異動清單 size=" + modifiedFiles.size());
            
            DiffGenerator diffGenerator = new DiffGenerator(clientManager);
            diffGenerator.generateDiffs(url, modifiedFiles, startRevision, endRevision, outputDir, preserveFileStructure);
            System.out.println("diff檔已產生完成 size=" + modifiedFiles.size());
            
            FileExporter fileExporter = new FileExporter(clientManager);
            fileExporter.exportFiles(url, modifiedFiles, startRevision, endRevision, exportDir);
            System.out.println("source已匯出完成 size=" + modifiedFiles.size());
            
            ExcelReportGenerator reportGenerator = new ExcelReportGenerator();
            reportGenerator.generateReport(modifiedFiles, reportPath, startRevision, endRevision, exportDir, url, clientManager);
            System.out.println("程式變更單已成功建立 size=" + modifiedFiles.size());
            
        } catch (SVNException | IOException e) {
            e.printStackTrace();
        }
    }
}
