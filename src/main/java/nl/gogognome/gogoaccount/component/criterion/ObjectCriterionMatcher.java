package nl.gogognome.gogoaccount.component.criterion;

import nl.gogognome.textsearch.criteria.Criterion;
import nl.gogognome.textsearch.string.CriterionMatcher;
import nl.gogognome.textsearch.string.StringSearchFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ObjectCriterionMatcher {

    private final CriterionMatcher criterionMatcher;

    public ObjectCriterionMatcher() {
        this(new StringSearchFactory().caseInsensitiveCriterionMatcher());
    }

    ObjectCriterionMatcher(CriterionMatcher criterionMatcher) {
        this.criterionMatcher = criterionMatcher;
    }

    public boolean matches(Criterion criterion, Object... params) {
        int resultSize = countResultingSize(params);
        String[] textElements = convertToStrings(resultSize, params);
        return criterionMatcher.matches(criterion, textElements);
    }

    private int countResultingSize(Object[] params) {
        int resultSize = 0;
        for (Object p : params) {
            if (p instanceof Date) {
                resultSize += 4;
            } else {
                resultSize += 1;
            }
        }
        return resultSize;
    }

    private String[] convertToStrings(int resultSize, Object[] params) {
        String[] textElements = new String[resultSize];
        int index = 0;
        for (Object p : params) {
            if (p instanceof Date) {
                Date date = (Date) p;
                textElements[index++] = formatDate("yyyyMMdd", date);
                textElements[index++] = formatDate("yyyy-MM-dd", date);
                textElements[index++] = formatDate("ddMMyyyy", date);
                textElements[index++] = formatDate("dd-MM-yyyy", date);
            } else {
                textElements[index++] = p != null ? p.toString() : null;
            }
        }
        return textElements;
    }

    private String formatDate(String dateFormat, Date date) {
        return date == null ? null : new SimpleDateFormat(dateFormat).format(date);
    }

}
