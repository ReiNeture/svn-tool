package fubuki.ref.cellstyle;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Workbook;

public class CellStyleFactory {

    private final Workbook workbook;
    private final Font contentFont;
    
    private final CellStyle contentCellStyle;
    private final CellStyle addedCellStyle;
    private final CellStyle deletedCellStyle;
    private final CellStyle unknownCellStyle;
    
    public CellStyleFactory(Workbook workbook) {
        this.workbook = workbook;
        this.contentFont = workbook.createFont();
        contentFont.setFontName("Verdana");
        contentFont.setFontHeightInPoints((short) 10);
        
        this.contentCellStyle = createCellStyle(CellStyleType.CONTENT);
        this.addedCellStyle = createCellStyle(CellStyleType.ADDED);
        this.deletedCellStyle = createCellStyle(CellStyleType.DELETED);
        this.unknownCellStyle = createCellStyle(CellStyleType.UNKNOWN);
    }

    public CellStyle createCellStyle(CellStyleType type) {
        CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setFont(contentFont);
        cellStyle.setAlignment(HorizontalAlignment.LEFT);
        cellStyle.setFillForegroundColor(type.getColor());
        cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return cellStyle;
    }
    
    public CellStyle getContentCellStyle() {
        return contentCellStyle;
    }

    public CellStyle getAddedCellStyle() {
        return addedCellStyle;
    }

    public CellStyle getDeletedCellStyle() {
        return deletedCellStyle;
    }
    
    public CellStyle getUnknownCellStyle() {
        return unknownCellStyle;
    }
    
}
