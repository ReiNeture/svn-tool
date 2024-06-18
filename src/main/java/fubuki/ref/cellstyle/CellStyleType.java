package fubuki.ref.cellstyle;

import org.apache.poi.xssf.usermodel.XSSFColor;

public enum CellStyleType {
    CONTENT(new XSSFColor(new byte[] {(byte) 182, (byte) 221, (byte) 232})),
    ADDED(new XSSFColor(new byte[] {(byte) 181, (byte) 230, (byte) 162})),
    DELETED(new XSSFColor(new byte[] {(byte) 218, (byte) 150, (byte) 148})),
    UNKNOWN(new XSSFColor(new byte[] {(byte) 166, (byte) 166, (byte) 166}));

    private final XSSFColor color;

    CellStyleType(XSSFColor color) {
        this.color = color;
    }

    public XSSFColor getColor() {
        return color;
    }
}
