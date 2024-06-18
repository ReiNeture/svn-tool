package fubuki.ref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntryPath;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.wc.SVNClientManager;

import fubuki.ref.cellstyle.CellStyleFactory;
import fubuki.ref.entry.ModifiedFileEntry;

public class ExcelReportGenerator {

    private final CellStyleFactory styleFactory;
    private final Workbook workbook;

    public ExcelReportGenerator() {
        this.workbook = new XSSFWorkbook();
        this.styleFactory = new CellStyleFactory(workbook);
    }
	
    public void generateReport(List<ModifiedFileEntry> modifiedFiles, String outputFilePath, long startRevision, long endRevision, String sourceDir, SVNURL url, SVNClientManager clientManager) throws IOException, SVNException {
        Sheet sheet = workbook.createSheet("SVN Changes");

        // 設定字體和表頭樣式
        Font headerFont = workbook.createFont();
        headerFont.setFontName("新細明體");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
        headerCellStyle.setFillForegroundColor(new XSSFColor(new byte[] {(byte) 255, (byte) 255, (byte) 153})); // 背景顏色 255 255 153
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerCellStyle.setBorderTop(BorderStyle.THIN);
        headerCellStyle.setBorderBottom(BorderStyle.THIN);
        headerCellStyle.setBorderLeft(BorderStyle.THIN);
        headerCellStyle.setBorderRight(BorderStyle.THIN);
        
        // 創建表頭
        String[] headers = {"影響的輸出物件", "序號", "儲存路徑", "操作類型", "修改日期：時間", "程式大小（位元組）", "版本 - [SVN]", "版本 - [Edit History]"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerCellStyle);
        }

        // 設定日期格式
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 aahh時mm分ss秒");

        // 填充數據
        int rowNum = 1;
        int index = 1;
        for (ModifiedFileEntry modifiedFileEntry : modifiedFiles) {
            SVNLogEntryPath entry = modifiedFileEntry.getEntryPath();
            String realModificationType = determineRealModificationType(modifiedFileEntry.getOperations());

            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(entry.getPath().substring(entry.getPath().lastIndexOf('/') + 1)); // 檔案名稱
            row.createCell(1).setCellValue(index++); // 序號
            row.createCell(2).setCellValue(entry.getPath().substring(0, entry.getPath().lastIndexOf('/'))); // 儲存路徑
            row.createCell(3).setCellValue(realModificationType); // 操作類型
            row.createCell(4).setCellValue(sdf.format(modifiedFileEntry.getCommitDate())); // 修改日期：時間
            row.createCell(5).setCellValue(entry.getType() == 'D' ? 0 : getFileSize(sourceDir, entry.getPath())); // 程式大小
            row.createCell(6).setCellValue(startRevision); // 版本 - [SVN]
            row.createCell(7).setCellValue(endRevision); // 版本 - [Edit History]
            
            // 根據操作類型設置行的樣式
            CellStyle rowStyle = styleFactory.getContentCellStyle(); // 默認樣式
            if (realModificationType.equals("新增")) {
                rowStyle = styleFactory.getAddedCellStyle();
            } else if (realModificationType.equals("刪除")) {
                rowStyle = styleFactory.getDeletedCellStyle();
            } else if (realModificationType.equals("未知")) {
            	rowStyle = styleFactory.getUnknownCellStyle();
            }
            
            for (int i = 0; i < headers.length; i++) {
                row.getCell(i).setCellStyle(rowStyle);
            }
        }

        // 自動調整欄寬
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // 寫入 Excel 文件
        try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
            workbook.write(fileOut);
        }

        workbook.close();
    }

    private long getFileSize(String sourceDir, String filePath) {
        File file = new File(sourceDir, filePath);
        if (file.exists() && file.isFile()) {
            return file.length();
        } else {
            return 0;
        }
    }

    private String getTypeDescription(char type) {
        switch (type) {
            case SVNLogEntryPath.TYPE_ADDED:
                return "新增";
            case SVNLogEntryPath.TYPE_DELETED:
                return "刪除";
            case SVNLogEntryPath.TYPE_MODIFIED:
                return "修改";
            case SVNLogEntryPath.TYPE_REPLACED:
                return "取代";
            default:
                return "未知";
        }
    }
    
    private String determineRealModificationType(List<Character> operations) {
        char firstType = operations.get(0);
        char lastType = operations.get(operations.size() - 1);

        if (firstType == SVNLogEntryPath.TYPE_ADDED && lastType == SVNLogEntryPath.TYPE_DELETED) {
            return "未知";
        } else if (firstType == SVNLogEntryPath.TYPE_ADDED && lastType == SVNLogEntryPath.TYPE_MODIFIED) {
            return "新增";
        } else if (firstType == SVNLogEntryPath.TYPE_ADDED && lastType == SVNLogEntryPath.TYPE_ADDED) {
            return "新增";
        } else if (firstType == SVNLogEntryPath.TYPE_MODIFIED && lastType == SVNLogEntryPath.TYPE_DELETED) {
            return "刪除";
        } else if (firstType == SVNLogEntryPath.TYPE_MODIFIED && lastType == SVNLogEntryPath.TYPE_MODIFIED) {
            return "修改";
        } else if (firstType == SVNLogEntryPath.TYPE_MODIFIED && lastType == SVNLogEntryPath.TYPE_ADDED) {
            return "修改";
        } else if (firstType == SVNLogEntryPath.TYPE_DELETED && lastType == SVNLogEntryPath.TYPE_DELETED) {
            return "刪除";
        } else if (firstType == SVNLogEntryPath.TYPE_DELETED && lastType == SVNLogEntryPath.TYPE_MODIFIED) {
            return "修改";
        } else if (firstType == SVNLogEntryPath.TYPE_DELETED && lastType == SVNLogEntryPath.TYPE_ADDED) {
            return "修改";
        } else {
            return getTypeDescription(firstType);
        }
    }
}
