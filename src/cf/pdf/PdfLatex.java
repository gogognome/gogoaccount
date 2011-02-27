/*
    This file is part of gogo account.

    gogo account is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    gogo account is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with gogo account.  If not, see <http://www.gnu.org/licenses/>.
*/
package cf.pdf;

import java.io.File;
import java.io.IOException;

/**
 *
 *
 * @author Sander Kooijmans
 */
public class PdfLatex {

    /** Private constructor because no instance should be created. */
    private PdfLatex() {
        throw new InternalError("This constructor must not be called");
    }

    /**
     * Converts a tex file to a PDF file.
     *
     * @param texFile the tex file.
     * @param pdfFileDirectory the directory in which the PDF file is to be
     *         created.
     * @throws IOException if an I/O error occurs.
     * @throws InterruptedException if the conversion is interrupted.
     */
    public static void convertTexToPdf(File texFile, File pdfFileDirectory)
    		throws IOException, InterruptedException {
        String command = "pdflatex -interaction=nonstopmode "
            + "-output-directory " + pdfFileDirectory.getAbsolutePath()
            + " " + texFile.getAbsolutePath();
        if (Runtime.getRuntime().exec(command).waitFor() != 0) {
            throw new IOException("Conversion from Tex to PDF has failed.");
        }
    }

    /**
     * Checks whether the PDF converter is available.
     * @return <code>true</code> if it is available; <code>false</code> otherwise
     */
    public static boolean pdfConverterAvailable() {
        String command = "pdflatex -version";
        boolean pdfConverterPresent = false;
        try {
            Process process = Runtime.getRuntime().exec(command);
            pdfConverterPresent = process.waitFor() == 0;
        } catch (InterruptedException e) {
        } catch (IOException e) {
        }
        return pdfConverterPresent;
    }

    /**
     * Gets the name of the tex file that corresponds to a PDF file.
     * @param pdfFileName the name of the PDF file
     * @return the name of the tex file
     */
    public static String getTexFileName(String pdfFileName) {
        String baseFileName = pdfFileName;
        if (baseFileName.toLowerCase().endsWith(".pdf")) {
            baseFileName = baseFileName.substring(0, baseFileName.length() - 4);
        }

        return baseFileName + ".tex";
    }

    /**
     * Converts unicode text to a tex representation.
     * Letters with accents are translated into the corresponding tex representation.
     * @param text the uncode text
     * @return the tex representation of the text
     */
    public static String convertUnicodeToTex(String text) {
        StringBuffer sb = new StringBuffer(text);
        replace(sb, "ä", "\"a");
        replace(sb, "ë", "\"e");
        replace(sb, "ï", "\"i");
        replace(sb, "ö", "\"o");
        replace(sb, "ü", "\"u");
        replace(sb, "ÿ", "\"y");
        replace(sb, "Ä", "\"A");
        replace(sb, "Ë", "\"E");
        replace(sb, "Ï", "\"I");
        replace(sb, "Ö", "\"O");
        replace(sb, "Ü", "\"U");
        replace(sb, "à", "`a");
        replace(sb, "è", "`e");
        replace(sb, "ì", "`i");
        replace(sb, "ò", "`o");
        replace(sb, "ù", "`u");
        replace(sb, "á", "'a");
        replace(sb, "é", "'e");
        replace(sb, "ó", "'o");
        replace(sb, "ú", "'u");
        return sb.toString();
    }

    /**
     * Replaces all occurrences of <code>oldValue</code> with <code>newValue</code>
     * in the specified string buffer.
     * @param sb the string buffer
     * @param oldValue the old value
     * @param newValue the new value
     */
    private static void replace(StringBuffer sb, String oldValue, String newValue) {
        for (int index = sb.indexOf(oldValue); index != -1; index = sb.indexOf(oldValue)) {
            sb.replace(index, index + oldValue.length(), newValue);
        }
    }

}
