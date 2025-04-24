package com.sho.ss.asuna.engine.extension.configurable;

import com.sho.ss.asuna.engine.core.Page;
import com.sho.ss.asuna.engine.core.Site;
import com.sho.ss.asuna.engine.core.processor.PageProcessor;
import com.sho.ss.asuna.engine.core.utils.Experimental;

import java.util.List;

/**
 * @author code4crafter@gmail.com <br>
 */
@Experimental
public class ConfigurablePageProcessor implements PageProcessor
{

    private Site site;

    private List<ExtractRule> extractRules;

    public ConfigurablePageProcessor(Site site, List<ExtractRule> extractRules)
    {
        this.site = site;
        this.extractRules = extractRules;
    }

    @Override
    public void process(Page page)
    {
        for (ExtractRule extractRule : extractRules)
        {
            if (extractRule.isMulti())
            {
                    List<String> results = page.getHtml().selectDocumentForList(extractRule.getSelector());
                    if (extractRule.isNotNull() && results.isEmpty())
                    {
                        page.setSkip(true);
                    } else
                    {
                        page.getResultItems().put(extractRule.getFieldName(), results);
                    }
            } else
            {

                try
                {
                    String result = page.getHtml().selectDocument(extractRule.getSelector());
                    if (extractRule.isNotNull() && result == null)
                    {
                        page.setSkip(true);
                    } else
                    {
                        page.getResultItems().put(extractRule.getFieldName(), result);
                    }
                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public Site getSite()
    {
        return site;
    }

}
