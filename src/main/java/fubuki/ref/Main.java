package fubuki.ref;

import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import org.apache.commons.io.output.TeeOutputStream;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import fubuki.ref.entry.ModifiedFileEntry;

public class Main {

    public static void main(String[] args) {
    	// SVN設定
        final String repoUrl = "https://192.168.18.41/svn/YuukiKitsu/";  // your svn repo URL
        final String branchPath = "/trunk"; // 指定的分支路徑
        final String revisionRange = "17-19";
        final long customStartRevision = -1; // 自訂的開始版本號, 設為 -1 代表使用 revisionRange
        // 輸出設定
        final String outputDir = "./export/svn_diffs";
        final String exportDir = "./export/svn_source";
        final String reportPath = "./export/svn_report.xlsx";
        final String logPath = "./export/console.log";
        // 其他設定
        final boolean preserveFileStructure = false; // diff need to create directory structure for files?
        
        
        redirectOutput(logPath);
        List<Long> revisions = SVNUtilities.parseRevisions(revisionRange);
        final long startRevision = (customStartRevision < 0) ? revisions.get(0) - 1 : customStartRevision;
        final long endRevision = revisions.get(revisions.size() - 1);
        
        try {
            SVNURL url = SVNURL.parseURIEncoded(repoUrl);
            SVNClientManager clientManager = SVNClientManager.newInstance();
            
            List<ModifiedFileEntry> modifiedFiles = SVNUtilities.getModifiedFiles(url, revisionRange, clientManager, branchPath);
            System.out.println("已取得版本異動清單 size=" + modifiedFiles.size());
            
            DiffGenerator diffGenerator = new DiffGenerator(clientManager);
            diffGenerator.generateDiffs(url, modifiedFiles, startRevision, endRevision, outputDir, preserveFileStructure);
            System.out.println("diff檔已產生完成 size=" + modifiedFiles.size());
            
            FileExporter fileExporter = new FileExporter(clientManager);
            fileExporter.exportFiles(url, modifiedFiles, endRevision, exportDir);
            System.out.println("source已匯出完成 size=" + modifiedFiles.size());
            
            ExcelReportGenerator reportGenerator = new ExcelReportGenerator();
            reportGenerator.generateReport(modifiedFiles, reportPath, startRevision, endRevision, exportDir, url, clientManager);
            System.out.println("程式變更單已成功建立 size=" + modifiedFiles.size());
            
            // 打開檔案總管
            openParentDirInExplorer(outputDir);
            
        } catch (SVNException | IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void redirectOutput(String logPath) {
        try {
            PrintStream console = System.out;
            PrintStream logStream = new PrintStream(new FileOutputStream(logPath));
            TeeOutputStream teeOut = new TeeOutputStream(console, logStream);
            TeeOutputStream teeErr = new TeeOutputStream(System.err, logStream);

            System.setOut(new PrintStream(teeOut));
            System.setErr(new PrintStream(teeErr));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private static void openParentDirInExplorer(String outputDir) {
        try {
            File dir = new File(outputDir).getAbsoluteFile().getParentFile();
            if (dir == null || !dir.exists()) {
                System.out.println("目錄不存在: " + dir);
                return;
            }
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(dir);
            } else {
                System.out.println("Desktop 不支援自動開啟檔案總管.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
