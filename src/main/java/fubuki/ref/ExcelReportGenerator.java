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

    private final Workbook workbook;
    private final CellStyle headerCellStyle;
    private final CellStyle rightAlignCellStyle;
    private final CellStyle centerAlignCellStyle;
    private final CellStyleFactory styleFactory;

    public ExcelReportGenerator() {
        this.workbook = new XSSFWorkbook();
        this.styleFactory = new CellStyleFactory(workbook);
        this.headerCellStyle = createHeaderCellStyle();
        this.rightAlignCellStyle = createCellStyle(HorizontalAlignment.RIGHT);
        this.centerAlignCellStyle = createCellStyle(HorizontalAlignment.CENTER);
    }

    private CellStyle createHeaderCellStyle() {
        Font headerFont = workbook.createFont();
        headerFont.setFontName("新細明體");
        headerFont.setFontHeightInPoints((short) 10);
        headerFont.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(headerFont);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte) 255, (byte) 255, (byte) 153}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);

        return style;
    }

    private CellStyle createCellStyle(HorizontalAlignment alignment) {
        CellStyle style = workbook.createCellStyle();
        style.setAlignment(alignment);
        return style;
    }

    public void generateReport(List<ModifiedFileEntry> modifiedFiles, String outputFilePath, long startRevision, long endRevision, String sourceDir, SVNURL url, SVNClientManager clientManager) throws IOException, SVNException {
        Sheet sheet = workbook.createSheet("SVN Changes");

        // 創建表頭
        String[] headers = {"影響的輸出物件", "序號", "儲存路徑", "操作類型", "修改日期：時間", "程式大小（位元組）", "比對版本 - [SVN]", "最後修改版本 - [SVN]", "備註"};
        createHeaderRow(sheet, headers);

        // 填充數據
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日 aahh時mm分ss秒");
        int rowNum = 1;
        int index = 1;

        for (ModifiedFileEntry modifiedFileEntry : modifiedFiles) {
            Row row = sheet.createRow(rowNum++);
            fillRow(row, modifiedFileEntry, index++, sdf, sourceDir, startRevision);
        }

        // 自動調整欄寬
        autoSizeColumns(sheet, headers.length);

        // 寫入 Excel 文件
        try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
            workbook.write(fileOut);
        }

        workbook.close();
    }

    private void createHeaderRow(Sheet sheet, String[] headers) {
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerCellStyle);
        }
    }

    private void fillRow(Row row, ModifiedFileEntry modifiedFileEntry, int index, SimpleDateFormat sdf, String sourceDir, long startRevision) {
        SVNLogEntryPath entry = modifiedFileEntry.getEntryPath();
        String realModificationType = determineRealModificationType(modifiedFileEntry.getOperations());

        row.createCell(0).setCellValue(entry.getPath().substring(entry.getPath().lastIndexOf('/') + 1));
        row.createCell(1).setCellValue(index);
        row.createCell(2).setCellValue(entry.getPath().substring(0, entry.getPath().lastIndexOf('/')));

        Cell operationCell = row.createCell(3);
        operationCell.setCellValue(realModificationType);
        operationCell.setCellStyle(centerAlignCellStyle);

        row.createCell(4).setCellValue(sdf.format(modifiedFileEntry.getCommitDate()));

        Cell fileSizeCell = row.createCell(5);
        fileSizeCell.setCellValue(entry.getType() == 'D' ? 0 : getFileSize(sourceDir, entry.getPath()));
        fileSizeCell.setCellStyle(rightAlignCellStyle);

        Cell startRevisionCell = row.createCell(6);
        startRevisionCell.setCellValue(startRevision);
        startRevisionCell.setCellStyle(rightAlignCellStyle);

        Cell lastCommitRevisionCell = row.createCell(7);
        lastCommitRevisionCell.setCellValue(modifiedFileEntry.getLastCommitRevision());
        lastCommitRevisionCell.setCellStyle(rightAlignCellStyle);

        row.createCell(8).setCellValue("");

        // 根據操作類型設置行的樣式
        CellStyle rowStyle = styleFactory.getContentCellStyle();
        if (realModificationType.equals("新增")) {
            rowStyle = styleFactory.getAddedCellStyle();
        } else if (realModificationType.equals("刪除")) {
            rowStyle = styleFactory.getDeletedCellStyle();
        } else if (realModificationType.equals("未知")) {
            rowStyle = styleFactory.getUnknownCellStyle();
        }

        applyRowStyle(row, rowStyle);
    }

    private void applyRowStyle(Row row, CellStyle rowStyle) {
        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (i != 3 && i != 5 && i != 6 && i != 7) {
                cell.setCellStyle(rowStyle);
            }
        }
        row.getCell(3).setCellStyle(rowStyle);
        row.getCell(5).setCellStyle(rowStyle);
        row.getCell(6).setCellStyle(rowStyle);
        row.getCell(7).setCellStyle(rowStyle);
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private long getFileSize(String sourceDir, String filePath) {
        File file = new File(sourceDir, filePath);
        return file.exists() && file.isFile() ? file.length() : 0;
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
        } else {
            return "未知";
        }
    }
}
