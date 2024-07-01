package fubuki.ref;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNLogEntry;
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

    public void generateReport(List<ModifiedFileEntry> modifiedFiles, String outputFilePath, long startRevision, long endRevision, String sourceDir, SVNURL url, 
        SVNClientManager clientManager, boolean includeFullHistory, long customStartRevision) throws IOException, SVNException {

        Sheet sheet = workbook.createSheet("SVN Changes");
        createHeaderRow(sheet);
        fillData(sheet, modifiedFiles, startRevision, endRevision, sourceDir, url, clientManager, includeFullHistory, customStartRevision);
        autoSizeColumns(sheet);
        writeToFile(outputFilePath);
    }

    private void createHeaderRow(Sheet sheet) {
        Font headerFont = workbook.createFont();
        headerFont.setFontName("新細明體");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        headerCellStyle.setAlignment(HorizontalAlignment.CENTER);
        headerCellStyle.setFillForegroundColor(new XSSFColor(new byte[] {(byte) 255, (byte) 255, (byte) 153}));
        headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerCellStyle.setBorderTop(BorderStyle.THIN);
        headerCellStyle.setBorderBottom(BorderStyle.THIN);
        headerCellStyle.setBorderLeft(BorderStyle.THIN);
        headerCellStyle.setBorderRight(BorderStyle.THIN);

        String[] headers = {"影響的輸出物件", "序號", "儲存路徑", "操作類型", "修改日期：時間", "程式大小（位元組）", "比較版本 - [SVN]", "修改版本 - [SVN]", "備註"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerCellStyle);
        }
    }

    private void fillData(Sheet sheet, List<ModifiedFileEntry> modifiedFiles, long startRevision, long endRevision, String sourceDir, SVNURL url, 
        SVNClientManager clientManager, boolean includeFullHistory, long customStartRevision) throws SVNException {
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 aahh時mm分ss秒");
        int rowNum = 1;
        int index = 1;
        
        for (ModifiedFileEntry modifiedFileEntry : modifiedFiles) {
            SVNLogEntryPath entry = modifiedFileEntry.getEntryPath();
            String realModificationType = determineRealModificationType(entry, modifiedFileEntry, includeFullHistory, customStartRevision, endRevision, url, clientManager);
            createDataRow(sheet, rowNum++, index++, entry, realModificationType, modifiedFileEntry, sdf, startRevision, sourceDir);
        }
    }

    private void createDataRow(Sheet sheet, int rowNum, int index, SVNLogEntryPath entry, String realModificationType, ModifiedFileEntry modifiedFileEntry, 
        SimpleDateFormat sdf, long startRevision, String sourceDir) {

        Row row = sheet.createRow(rowNum);
        row.createCell(0).setCellValue(entry.getPath().substring(entry.getPath().lastIndexOf('/') + 1));
        row.createCell(1).setCellValue(index);
        row.createCell(2).setCellValue(entry.getPath().substring(0, entry.getPath().lastIndexOf('/')));
        row.createCell(3).setCellValue(realModificationType);
        row.createCell(4).setCellValue(sdf.format(modifiedFileEntry.getCommitDate()));
        row.createCell(5).setCellValue(entry.getType() == 'D' ? 0 : getFileSize(sourceDir, entry.getPath()));
        row.createCell(6).setCellValue(startRevision);
        row.createCell(7).setCellValue(modifiedFileEntry.getLastCommitRevision());
        row.createCell(8).setCellValue("");

        CellStyle rowStyle = getRowStyle(realModificationType);
        for (int i = 0; i < 9; i++) {
            row.getCell(i).setCellStyle(rowStyle);
        }
    }

    private String determineRealModificationType(SVNLogEntryPath entry, ModifiedFileEntry modifiedFileEntry, boolean includeFullHistory, 
        long customStartRevision, long endRevision, SVNURL url, SVNClientManager clientManager) throws SVNException {
        
        if (includeFullHistory && customStartRevision != -1) {
            List<SVNLogEntry> fullHistory = SVNUtilities.getFileHistory(url, entry.getPath(), customStartRevision, endRevision, clientManager);
            List<Character> operations = fullHistory.stream()
                    .flatMap(logEntry -> logEntry.getChangedPaths().values().stream())
                    .filter(path -> path.getPath().equals(entry.getPath()))
                    .map(SVNLogEntryPath::getType)
                    .collect(Collectors.toList());
                    
            return determineRealModificationType(operations);
        } else {
            return determineRealModificationType(modifiedFileEntry.getOperations());
        }
    }

    private void autoSizeColumns(Sheet sheet) {
        for (int i = 0; i < 9; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void writeToFile(String outputFilePath) throws IOException {
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

    private CellStyle getRowStyle(String realModificationType) {
        CellStyle rowStyle = styleFactory.getContentCellStyle();
        if (realModificationType.equals("新增")) {
            rowStyle = styleFactory.getAddedCellStyle();
        } else if (realModificationType.equals("刪除")) {
            rowStyle = styleFactory.getDeletedCellStyle();
        } else if (realModificationType.equals("未知")) {
            rowStyle = styleFactory.getUnknownCellStyle();
        }
        return rowStyle;
    }
}
