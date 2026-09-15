package com.saasai.extractor;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
public class XlsxFileExtractor implements FileExtractor {

    @Override
    public boolean supports(String extension) {
        return extension != null
                && "xlsx".equalsIgnoreCase(extension.trim());
    }

    @Override
    public ExtractResult extract(Path filePath) throws IOException {
        validatePath(filePath);

        StringBuilder rawText = new StringBuilder();

        int sheetCount = 0;
        int rowCount = 0;
        int cellCount = 0;

        DataFormatter dataFormatter = new DataFormatter();

        try (
                InputStream inputStream =
                        new BufferedInputStream(Files.newInputStream(filePath));

                Workbook workbook =
                        WorkbookFactory.create(inputStream)
        ) {
            FormulaEvaluator formulaEvaluator =
                    workbook.getCreationHelper().createFormulaEvaluator();

            for (Sheet sheet : workbook) {
                sheetCount++;

                rawText.append("[SHEET: ")
                        .append(sheet.getSheetName())
                        .append("]")
                        .append(System.lineSeparator());

                for (Row row : sheet) {
                    rowCount++;

                    boolean firstCell = true;

                    for (Cell cell : row) {
                        String cellValue = dataFormatter.formatCellValue(
                                cell,
                                formulaEvaluator
                        );

                        if (!firstCell) {
                            rawText.append('\t');
                        }

                        rawText.append(cellValue);
                        firstCell = false;
                        cellCount++;
                    }

                    rawText.append(System.lineSeparator());
                }

                rawText.append(System.lineSeparator());
            }
        } catch (Exception exception) {
            if (exception instanceof IOException ioException) {
                throw ioException;
            }

            throw new IOException(
                    "Không thể bóc nội dung file XLSX: "
                            + filePath.getFileName(),
                    exception
            );
        }

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("fileType", "XLSX");
        metadata.put("sheetCount", sheetCount);
        metadata.put("rowCount", rowCount);
        metadata.put("cellCount", cellCount);

        return ExtractResult.builder()
                .rawText(rawText.toString())
                .metadata(metadata)
                .build();
    }

    private void validatePath(Path filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException(
                    "Đường dẫn file XLSX không được để trống"
            );
        }

        if (!Files.exists(filePath)) {
            throw new IllegalArgumentException(
                    "File XLSX không tồn tại: " + filePath
            );
        }

        if (!Files.isRegularFile(filePath)) {
            throw new IllegalArgumentException(
                    "Đường dẫn XLSX không phải file hợp lệ: " + filePath
            );
        }
    }
}