package com.sho.ss.asuna.engine.core.selector;


import com.sho.ss.asuna.engine.core.utils.Experimental;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Borrowed from https://code.google.com/p/cx-extractor/
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.4.1
 *
 */
@Experimental
public class SmartContentSelector implements Selector {

    public SmartContentSelector() {
    }

    @Override
    public String select(String html) {
        html = html.replaceAll("(?is)<!DOCTYPE.*?>", "");
        html = html.replaceAll("(?is)<!--.*?-->", "");				// remove html comment
        html = html.replaceAll("(?is)<script.*?>.*?</script>", ""); // remove javascript
        html = html.replaceAll("(?is)<style.*?>.*?</style>", "");   // remove css
        html = html.replaceAll("&.{2,5};|&#.{2,5};", " ");			// remove special char
        html = html.replaceAll("(?is)<.*?>", "");
        List<String> lines;
        int blocksWidth =3;
        int threshold =86;
        int start;
        int end;
        StringBuilder text = new StringBuilder();
        ArrayList<Integer> indexDistribution = new ArrayList<Integer>();

        lines = Arrays.asList(html.split("\n"));

        for (int i = 0; i < lines.size() - blocksWidth; i++) {
            int wordsNum = 0;
            for (int j = i; j < i + blocksWidth; j++) {
                lines.set(j, lines.get(j).replaceAll("\\s+", ""));
                wordsNum += lines.get(j).length();
            }
            indexDistribution.add(wordsNum);
        }

        start = -1; end = -1;
        boolean boolstart = false, boolend = false;
        text.setLength(0);

        for (int i = 0; i < indexDistribution.size() - 1; i++) {
            if (indexDistribution.get(i) > threshold && ! boolstart) {
                if (indexDistribution.get(i + 1) != 0
                        || indexDistribution.get(i + 2) != 0
                        || indexDistribution.get(i + 3) != 0) {
                    boolstart = true;
                    start = i;
                    continue;
                }
            }
            if (boolstart) {
                if (indexDistribution.get(i) == 0
                        || indexDistribution.get(i + 1) == 0) {
                    end = i;
                    boolend = true;
                }
            }
            StringBuilder tmp = new StringBuilder();
            if (boolend) {
                for (int ii = start; ii <= end; ii++) {
                    if (lines.get(ii).length() < 5) continue;
                    tmp.append(lines.get(ii)).append("\n");
                }
                String str = tmp.toString();
                if (str.contains("Copyright")   ) continue;
                text.append(str);
                boolstart = boolend = false;
            }
        }
        return text.toString();
    }

    @Override
    public List<String> selectList(String text) {
        throw new UnsupportedOperationException();
    }
}
